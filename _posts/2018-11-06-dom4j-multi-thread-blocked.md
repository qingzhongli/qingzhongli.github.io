---
layout: post
title: dom4j之多线程解析xml文件的锁问题
---

## 背景
线上程序是一个文本解析程序，固定30秒间隔扫描指定目录，针对每次扫描到的xml文件，配置了一个15线程的线程池，多线程去解析处理，每一个文件一个线程负责去处理。xml文件的读取使用的是dom4j，运行一段时间后，发现解析的比较慢，慢的表现一个是发现cpu核数没有充分利用起来，即，使用`top`命令看到只使用了6、7个核。

## 问题调查
### 获取进程快照
```sh
jstack <pid>  # 获取进程快照，以观察java进程在干什么
```
### 分析进程快照

经分析[快照](../files/blog/2018-11-06-dom4j-multi-thread-blocked-1.dump)，发现同时有13个线程竞争同一个锁对象，摘取其中一个竞争锁的线程信息，如下：
```
"pool-217-thread-15" prio=10 tid=0x00007f53144a1800 nid=0xb255 waiting for monitor entry [0x00007f43ab0f0000]
   java.lang.Thread.State: BLOCKED (on object monitor)
	at java.util.Collections$SynchronizedMap.get(Collections.java:2037)
	- waiting to lock <0x00007f44140280a8> (a java.util.Collections$SynchronizedMap)
	at org.dom4j.tree.QNameCache.get(QNameCache.java:117)
	at org.dom4j.DocumentFactory.createQName(DocumentFactory.java:199)
	at org.dom4j.tree.NamespaceStack.createQName(NamespaceStack.java:392)
	at org.dom4j.tree.NamespaceStack.pushQName(NamespaceStack.java:374)
	at org.dom4j.tree.NamespaceStack.getQName(NamespaceStack.java:213)
	at org.dom4j.io.SAXContentHandler.startElement(SAXContentHandler.java:234)
	at org.apache.xerces.parsers.AbstractSAXParser.startElement(AbstractSAXParser.java:454)
	at org.apache.xerces.impl.XMLNamespaceBinder.handleStartElement(XMLNamespaceBinder.java:876)
	at org.apache.xerces.impl.XMLNamespaceBinder.startElement(XMLNamespaceBinder.java:568)
	at org.apache.xerces.impl.dtd.XMLDTDValidator.startElement(XMLDTDValidator.java:756)
	at org.apache.xerces.impl.XMLDocumentFragmentScannerImpl.scanStartElement(XMLDocumentFragmentScannerImpl.java:752)
	at org.apache.xerces.impl.XMLDocumentFragmentScannerImpl$FragmentContentDispatcher.dispatch(XMLDocumentFragmentScannerImpl.java:1453)
	at org.apache.xerces.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:333)
	at org.apache.xerces.parsers.DTDConfiguration.parse(DTDConfiguration.java:524)
	at org.apache.xerces.parsers.DTDConfiguration.parse(DTDConfiguration.java:580)
	at org.apache.xerces.parsers.XMLParser.parse(XMLParser.java:152)
	at org.apache.xerces.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1169)
	at org.dom4j.io.SAXReader.read(SAXReader.java:465)
	at org.dom4j.io.SAXReader.read(SAXReader.java:343)
	......
	at com.lqz.parser.ParseThread.run(ParseThread.java:40)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615)
	at java.lang.Thread.run(Thread.java:745)
```
由上可得，被阻塞的13个线程都在竞争同一个锁（即，`0x00007f44140280a8`），根据入栈的顺序可知这些线程都是在解析xml文件(`org.dom4j.io.SAXReader.read(SAXReader.java:343)`)时导致了阻塞。那么就需要跟踪调试一下代码，从头到尾梳理一下整体流程，确认具体哪里处理导致的。
### 调试跟踪定位
由于是在执行`org.dom4j.tree.QNameCache.get(QNameCache.java:117)`时产生的，程序是maven项目，直接搜索打开类`QNameCache`，打上断点，启动程序进行调试跟踪。
```java
public QName get(String name, Namespace namespace) {
    Map cache = getNamespaceCache(namespace);
    QName answer = null;

    if (name != null) {
        answer = (QName) cache.get(name);  // 此行为第117行
    } else {
        name = "";
    }

    if (answer == null) {
        answer = createQName(name, namespace);
        answer.setDocumentFactory(documentFactory);
        cache.put(name, answer);
    }

    return answer;
}
```
操作的变量`cache`是`Map cache = getNamespaceCache(namespace)`
，即：
```java
protected Map getNamespaceCache(Namespace namespace) {
    if (namespace == Namespace.NO_NAMESPACE) {
        return noNamespaceCache;
    }

    Map answer = null;

    if (namespace != null) {
        answer = (Map) namespaceCache.get(namespace);
    }

    if (answer == null) {
        answer = createMap();
        namespaceCache.put(namespace, answer);
    }

    return answer;
}
```
操作的是`org.dom4j.tree.QNameCache`的全局变量`noNamespaceCache`，如下：
```java
public class QNameCache {
    /** Cache of {@link QName}instances with no namespace */
    protected Map noNamespaceCache = Collections.synchronizedMap(new WeakHashMap());

    /**
     * Cache of {@link Map}instances indexed by namespace which contain caches
     * of {@link QName}for each name
     */
    protected Map namespaceCache = Collections.synchronizedMap(new WeakHashMap());
    // ......
}
```
然而，noNamespaceCache是一个`SynchronizedMap`，该类在`get`时会针对使用一个对象锁`mutex`，如下：
```java
private static class SynchronizedMap<K,V>
    implements Map<K,V>, Serializable {
    private static final long serialVersionUID = 1978198479659022715L;

    private final Map<K,V> m;     // Backing Map
    final Object      mutex;        // Object on which to synchronize
    .....
    public V get(Object key) {
        synchronized (mutex) {return m.get(key);}
    }
    ......
}
```
但是，并发解析的处理逻辑是一个文件一个解析线程，线程之间怎么会产生竞争哪？合理的解释就是，QNameCache只有一个对象，那么从头到位调试分析一下，验证一下推断是否正确。首先是`new SAXReader().read(InputStream in)`，如下：
```java
SAXReader saxReader = new SAXReader();
Document domdoc = saxReader.read(new BufferedInputStream(inputstream));
```
而方法`org.dom4j.io.SAXReader.read(InputStream)`的定义是：
```java
public Document read(InputStream in) throws DocumentException {
    InputSource source = new InputSource(in);
    if (this.encoding != null) {
        source.setEncoding(this.encoding);
    }

    return read(source);
}
```
紧接着调用的是方法`org.dom4j.io.SAXReader.read(InputSource)`，即：
```java
public Document read(InputSource in) throws DocumentException {
     try {
         XMLReader reader = getXMLReader();

         reader = installXMLFilter(reader);

         EntityResolver thatEntityResolver = this.entityResolver;

         if (thatEntityResolver == null) {
             thatEntityResolver = createDefaultEntityResolver(in
                     .getSystemId());
             this.entityResolver = thatEntityResolver;
         }

         reader.setEntityResolver(thatEntityResolver);

         SAXContentHandler contentHandler = createContentHandler(reader);
         contentHandler.setEntityResolver(thatEntityResolver);
         contentHandler.setInputSource(in);

         boolean internal = isIncludeInternalDTDDeclarations();
         boolean external = isIncludeExternalDTDDeclarations();

         contentHandler.setIncludeInternalDTDDeclarations(internal);
         contentHandler.setIncludeExternalDTDDeclarations(external);
         contentHandler.setMergeAdjacentText(isMergeAdjacentText());
         contentHandler.setStripWhitespaceText(isStripWhitespaceText());
         contentHandler.setIgnoreComments(isIgnoreComments());
         reader.setContentHandler(contentHandler);

         configureReader(reader, contentHandler);

         reader.parse(in);

         return contentHandler.getDocument();
     } catch (Exception e) {
         // 省略......
     }
 }
```
其中关键的一句是`SAXContentHandler contentHandler = createContentHandler(reader)`，此处调用的是`org.dom4j.io.SAXReader.createContentHandler(XMLReader reader)`，即：
```java
protected SAXContentHandler createContentHandler(XMLReader reader) {
    return new SAXContentHandler(getDocumentFactory(), dispatchHandler);
}
```
其内部又调用了`org.dom4j.io.SAXReader.getDocumentFactory()`，即：
```java
public DocumentFactory getDocumentFactory() {
    if (factory == null) {
        factory = DocumentFactory.getInstance();
    }

    return factory;
}
```
内部调用了`org.dom4j.DocumentFactory.getInstance()`，截取`DocumentFactory`关键部分代码，如下：
```java
public class DocumentFactory implements Serializable {
    private static SingletonStrategy singleton = null;

    protected transient QNameCache cache;

    /** Default namespace prefix -> URI mappings for XPath expressions to use */
    private Map xpathNamespaceURIs;

    private static SingletonStrategy createSingleton() {
        SingletonStrategy result = null;

        String documentFactoryClassName;
        try {
            documentFactoryClassName = System.getProperty("org.dom4j.factory",
                    "org.dom4j.DocumentFactory");
        } catch (Exception e) {
            documentFactoryClassName = "org.dom4j.DocumentFactory";
        }

        try {
            String singletonClass = System.getProperty(
                    "org.dom4j.DocumentFactory.singleton.strategy",
                    "org.dom4j.util.SimpleSingleton");
            Class clazz = Class.forName(singletonClass);
            result = (SingletonStrategy) clazz.newInstance();
        } catch (Exception e) {
            result = new SimpleSingleton();
        }

        result.setSingletonClassName(documentFactoryClassName);

        return result;
    }

    public DocumentFactory() {
        init();
    }

    /**
     * <p>
     * Access to singleton implementation of DocumentFactory which is used if no
     * DocumentFactory is specified when building using the standard builders.
     * </p>
     *
     * @return the default singleon instance
     */
    public static synchronized DocumentFactory getInstance() {
        if (singleton == null) {
            singleton = createSingleton();
        }
        return (DocumentFactory) singleton.instance();
    }
    // ......省略
}
```
由上可知DocumentFactory是单例的，其全局变量cache（`protected transient QNameCache cache;`）自然也只有一份，`org.dom4j.tree.QNameCache`的全局变量`noNamespaceCache`和`namespaceCache`相应也只有一份，这就是解释了为何使用dom4j（`new SAXReader().read(file)`）多线程并发解析大量xml文件会导致线程阻塞了。

## 解决方法
多线程解析xml文件，彼此没有进行互斥的必要性，`org.dom4j.tree.QNameCache`的全局变量`noNamespaceCache`和`namespaceCache`，每个进程完全可以独立进行保存，因此可以将二者改成TheadLocal的WeakHashMap来替代，进而去掉竞争，提升并发解析的效率，弊端就是会每个线程有自己命名空间和非命名空间缓存，会消耗一定内存，以空间换时间，修正后代码[QNameCache.java](../files/blog/2018-11-06-dom4j-multi-thread-blocked-1-QNameCache.java)。

## 参照
* [记一次性能优化过程中的术与道](http://blog.longjiazuo.com/archives/5178)
* [QNameCache performance patch for Dom4j](https://issues.liferay.com/browse/LPS-7427)

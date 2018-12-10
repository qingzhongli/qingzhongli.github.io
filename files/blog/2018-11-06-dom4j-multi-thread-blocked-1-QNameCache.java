package org.dom4j.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.dom4j.DocumentFactory;
import org.dom4j.Namespace;
import org.dom4j.QName;

/**
 * <p>
 * <code>QNameCache</code> caches instances of <code>QName</code> for reuse
 * both across documents and within documents.
 * </p>< < < < < < < QNameCache.java
 */
public class QNameCache {
  /** Cache of {@link QName}instances with no namespace */
  protected ThreadLocal<Map> noNamespaceCache = new ThreadLocal() {
    protected Map initialValue() {
      return new WeakHashMap();
    }
  };

  /**
   * Cache of {@link Map}instances indexed by namespace which contain caches
   * of {@link QName}for each name
   */
  protected ThreadLocal<Map> namespaceCache = new ThreadLocal() {
    protected Map initialValue() {
      return new WeakHashMap();
    }
  };

  /**
   * The document factory associated with new QNames instances in this cache
   * or null if no instances should be associated by default
   */
  private DocumentFactory documentFactory;

  public QNameCache() {
  }

  public QNameCache(DocumentFactory documentFactory) {
    this.documentFactory = documentFactory;
  }

  /**
   * Returns a list of all the QName instances currently used in current thread
   *
   */
  public List getQNames() {
    List answer = new ArrayList();
    answer.addAll((this.noNamespaceCache.get()).values());

    for (Iterator it = (this.namespaceCache.get()).values().iterator(); it.hasNext(); ) {
      Map map = (Map) it.next();
      answer.addAll(map.values());
    }

    return answer;
  }

  /**
   * Return the QName for the given name and no namepsace in current thread
   */
  public QName get(String name) {
    QName answer = null;

    if (name != null) {
      answer = (QName) (this.noNamespaceCache.get()).get(name);
    } else {
      name = "";
    }

    if (answer == null) {
      answer = createQName(name);
      answer.setDocumentFactory(this.documentFactory);
      (this.noNamespaceCache.get()).put(name, answer);
    }

    return answer;
  }

  /**
   * Return the QName for the given local name and namepsace in current thread
   */
  public QName get(String name, Namespace namespace) {
    Map cache = getNamespaceCache(namespace);
    QName answer = null;

    if (name != null) {
      answer = (QName) cache.get(name);
    } else {
      name = "";
    }

    if (answer == null) {
      answer = createQName(name, namespace);
      answer.setDocumentFactory(this.documentFactory);
      cache.put(name, answer);
    }

    return answer;
  }

  /**
   * Return the QName for the given local name, qualified name and namepsace
   */
  public QName get(String localName, Namespace namespace, String qName) {
    Map cache = getNamespaceCache(namespace);
    QName answer = null;

    if (localName != null) {
      answer = (QName) cache.get(localName);
    } else {
      localName = "";
    }

    if (answer == null) {
      answer = createQName(localName, namespace, qName);
      answer.setDocumentFactory(this.documentFactory);
      cache.put(localName, answer);
    }

    return answer;
  }

  public QName get(String qualifiedName, String uri) {
    int index = qualifiedName.indexOf(':');

    if (index < 0) {
      return get(qualifiedName, Namespace.get(uri));
    }
    String name = qualifiedName.substring(index + 1);
    String prefix = qualifiedName.substring(0, index);

    return get(name, Namespace.get(prefix, uri));
  }

  /**
   * Return the cached QName instance if there is one or adds the given qname to the cache if not
   */
  public QName intern(QName qname) {
    return get(qname.getName(), qname.getNamespace(), qname.getQualifiedName());
  }

  protected Map getNamespaceCache(Namespace namespace) {
    if (namespace == Namespace.NO_NAMESPACE) {
      return this.noNamespaceCache.get();
    }

    Map answer = null;

    if (namespace != null) {
      answer = (Map) ((Map) this.namespaceCache.get()).get(namespace);
    }

    if (answer == null) {
      answer = createMap();
      ((Map) this.namespaceCache.get()).put(namespace, answer);
    }

    return answer;
  }

  protected Map createMap() {
    return new HashMap();
  }

  protected QName createQName(String name) {
    return new QName(name);
  }

  protected QName createQName(String name, Namespace namespace) {
    return new QName(name, namespace);
  }

  protected QName createQName(String name, Namespace namespace, String qualifiedName) {
    return new QName(name, namespace, qualifiedName);
  }
}

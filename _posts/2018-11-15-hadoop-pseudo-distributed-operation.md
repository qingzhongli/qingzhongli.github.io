---
layout: post
title: hadoop之伪分布式搭建
---
## 安装JDK
```sh
$ rpm jdk-8u181-linux-x64.rpm
```

## 配置hosts
```sh
[root@d1 hadoop-2.9.2]# vi /etc/hosts
127.0.0.1   localhost localhost.localdomain localhost4 localhost4.localdomain4
::1         localhost localhost.localdomain localhost6 localhost6.localdomain6

192.168.92.130 d1
[root@d1 hadoop-2.9.2]#
```

## 配置免密登录
```sh
$ ssh-keygen -t rsa
$ cat ~/.ssh/id_rsa.pub > ~/.ssh/authorized_keys
$ ssh d1 # 验证时候可以无密登录
```
## 下载安装包
[Apache Hadoop](https://hadoop.apache.org/releases.html) 官网下载hadoop安装包 hadoop-3.1.1，并解压。
```sh
[root@d1 local]# pwd
/usr/local
[root@d1 local]# tar -zxvf hadoop-3.1.1
[root@d1 local]# chown root:root -R hadoop-3.1.1
[root@d1 local]# ln -s hadoop-3.1.1 hadoop
```
## 修改配置

修改解压后的目录中的文件夹etc/hadoop下的xml配置文件（如果文件不存在，则自己创建）

hadoop-env.sh修改以下配置：

```sh
[root@d1 hadoop]# pwd
/usr/local/hadoop
[root@d1 hadoop]# vi etc/hadoop/hadoop-env.sh
export JAVA_HOME=/usr/java/jdk1.8.0_181-amd64

export HDFS_NAMENODE_USER=root
export HDFS_DATANODE_USER=root
export HDFS_SECONDARYNAMENODE_USER=root

export YARN_RESOURCEMANAGER_USER=root
export YARN_NODEMANAGER_USER=root
```

slaves文件修改为以下配置：

```sh
[root@d1 hadoop]# vi etc/hadoop/slaves
d1
```

注：以下四个XML配置文件，需在标签<configuration>和</configuration>之间增加配置项。

```sh
[root@d1 hadoop]# vi etc/hadoop/mapred-site.xml
<configuration>
  <property>
    <name>mapreduce.framework.name</name>
    <value>yarn</value>
  </property>
</configuration>
```

core-site.xml（其中“d1”是在/etc/hosts中设置的host，如果未设置，则换为localhost）：

```sh
[root@d1 hadoop]# vi etc/hadoop/core-site.xml
<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
    <property>
       <name>fs.default.name</name>
       <value>hdfs://d1:9000</value>
    </property>
    <property>
       <name>dfs.replication</name>
       <value>1</value>
    </property>
   <property>
      <name>hadoop.tmp.dir</name>
      <value>/hadoop/hadoop_tmp_dir</value>
    </property>
    <property>
       <name>dfs.namenode.name.dir</name>
       <value>/hadoop/dfs/name</value>
    </property>
    <property>
       <name>dfs.datanode.data.dir</name>
       <value>/hadoop/dfs/data</value>
    </property>
</configuration>
```

yarn-site.xml：

```sh
[root@d1 hadoop]# vi etc/hadoop/yarn-site.xml
<?xml version="1.0"?>
<configuration>
    <property>
        <name>yarn.nodemanager.aux-services</name>
        <value>mapreduce_shuffle</value>
    </property>
</configuration>
```

## 启动服务

格式化HDFS：
```sh
bin/hdfs namenode -format
```
启动HDFS：

```sh
sbin/start-dfs.sh
```

启动YARN：

```sh
sbin/start-yarn.sh
```

## 验证是否启动成功

```sh
[root@d1 hadoop]# jps
21697 Jps
20612 SecondaryNameNode
20308 NameNode
20933 NodeManager
20827 ResourceManager
20414 DataNode
[root@d1 hadoop]#
```
## 常见错误
若启动报本地类库无法加载，如下所示：
```
2018-11-21 18:28:37,260 WARN util.NativeCodeLoader: Unable to load native-hadoop library for your platform... using builtin-java classes where applicable
```
解决办法参照：[hadoop环境搭建常见问题](https://qingzhongli.com/hadoop-setup-common-problems)
## References
[Hadoop: Setting up a Single Node Cluster](http://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/SingleCluster.html)

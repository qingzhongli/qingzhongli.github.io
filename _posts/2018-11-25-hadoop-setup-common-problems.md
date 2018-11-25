---
layout: post
title: hadoop环境搭建常见问题
---
## 问题一：本地类库无法加载问题
### 问题描述
在搭建hadoop环境启动的时候，报无法加载本地类库的警告，具体信息如下：
```
WARN util.NativeCodeLoader: Unable to load native-hadoop library for your platform... using builtin-java classes where applicable
```
```
[root@d1 hadoop-2.9.2]# hadoop checknative -a
18/11/25 13:28:26 WARN util.NativeCodeLoader: Unable to load native-hadoop library for your platform... using builtin-java classes where applicable
Native library checking:
hadoop:  false
zlib:    false
snappy:  false
zstd  :  false
lz4:     false
bzip2:   false
openssl: false
18/11/25 13:28:27 INFO util.ExitUtil: Exiting with status 1: ExitException
[root@d1 hadoop-2.9.2]#
```
### 解决办法
参照 [Native Libraries Guide](http://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/NativeLibraries.html#Native_Hadoop_Library) ，自带本地hadoop类库支持系统有限（仅支持RHEL4/Fedora、Ubuntu、Gentoo），需要在搭建hadoop环境的机器，下载相应的hadoop源码，然后编译打包，使用编译后的本地hadoop类库。
#### 下载源码
在[Download](https://hadoop.apache.org/releases.html)页面找到网速比较快的分支，找到对应源码进行下载。本次实例中使用的是Hadoop2.9.2
```
wget http://mirrors.shu.edu.cn/apache/hadoop/common/hadoop-2.9.2/hadoop-2.9.2-src.tar.gz

tar -zxvf hadoop-2.9.2-src.tar.gz # 解压
```
#### 安装基础类库
```
yum install -y autoconf automake libtool
yum install -y gcc gcc-c++ cmake
yum install -y zlib-devel
yum install -y openssl-devel
yum install -y snappy snappy-devel
yum install -y bzip2 bzip2-devel
```

#### 安装protobuf
```
# 明确所需的protobuf版本，需要和hadoop源码中配置的版本保持一致
[root@d1 ~]# cd /usr/local/hadoop-2.9.2-src
[root@d1 hadoop-2.9.2-src]# grep 'protobuf.version' */pom.xml
hadoop-project/pom.xml:    <protobuf.version>2.5.0</protobuf.version>
hadoop-project/pom.xml:        <version>${protobuf.version}</version>
[root@d1 ~]#

# 下载所需的protobuf版本
[root@d1 ~]# wget https://github.com/google/protobuf/releases/download/v2.5.0/protobuf-2.5.0.tar.gz

# 安装所需的protobuf版本
[root@d1 ~]# tar zxvf protobuf-2.5.0.tar.gz -C /usr/local/src/
[root@d1 ~]# ll /usr/local/src/protobuf-2.5.0/
total 1696
-rw-r--r--.  1 109965 5000  36976 Feb 26  2013 aclocal.m4
-rwxr-xr--.  1 109965 5000   1519 Feb 26  2013 autogen.sh
-rw-r--r--.  1 109965 5000  25312 Feb 26  2013 CHANGES.txt
......
[root@d1 ~]# cd /usr/local/src/protobuf-2.5.0/
[root@d1 protobuf-2.5.0]# ./configure --prefix=/usr/local/protobuf
[root@d1 protobuf-2.5.0]# make && make install
[root@d1 protobuf-2.5.0]# /usr/local/protobuf/bin/protoc --version
libprotoc 2.5.0
[root@d1 bin]# init 6 # 重启
```

#### 安装zstd
```
[root@d1 ~]# cd /usr/local
[root@d1 local]# git clone -b master https://github.com/facebook/zstd.git
[root@d1 local]# cd zstd
[root@d1 zstd]# make && make install
[root@d1 zstd]# zstd --version
*** zstd command line interface 64-bits v1.3.7, by Yann Collet ***
[root@d1 zstd]# cp -rf lib/*.so* /usr/local/hadoop-2.9.2/lib/native/
```
#### 编译hadoop源码
```
[root@d1 hadoop-2.9.2-src]# mvn package -Pdist,native -DskipTests -Dtar
```
#### 替换自带本地类库
```
[root@d1 hadoop-2.9.2-src]# cp -rf hadoop-dist/target/hadoop-2.9.2/lib/native /usr/local/hadoop-2.9.2/lib
[root@d1 hadoop-2.9.2-src]# hadoop checknative -a
18/11/25 19:40:53 INFO bzip2.Bzip2Factory: Successfully loaded & initialized native-bzip2 library system-native
18/11/25 19:40:53 INFO zlib.ZlibFactory: Successfully loaded & initialized native-zlib library
Native library checking:
hadoop:  true /usr/local/hadoop-2.9.2/lib/native/libhadoop.so.1.0.0
zlib:    true /lib64/libz.so.1
snappy:  true /usr/lib64/libsnappy.so.1
zstd  :  true /usr/local/hadoop-2.9.2/lib/native/libzstd.so.1
lz4:     true revision:10301
bzip2:   true /lib64/libbz2.so.1
openssl: true /usr/lib64/libcrypto.so
```
#### 启动hadoop验证
```
[root@d1 hadoop-2.9.2-src]# cd /usr/local/hadoop-2.9.2
[root@d1 hadoop-2.9.2]# sbin/start-dfs.sh
Starting namenodes on [d1]
d1: starting namenode, logging to /usr/local/hadoop-2.9.2/logs/hadoop-root-namenode-d1.out
localhost: starting datanode, logging to /usr/local/hadoop-2.9.2/logs/hadoop-root-datanode-d1.out
Starting secondary namenodes [0.0.0.0]
0.0.0.0: starting secondarynamenode, logging to /usr/local/hadoop-2.9.2/logs/hadoop-root-secondarynamenode-d1.out
[root@d1 hadoop-2.9.2]# jps
4146 Jps
3732 DataNode
3542 NameNode
4030 SecondaryNameNode
[root@d1 hadoop-2.9.2]# sbin/start-yarn.sh
starting yarn daemons
starting resourcemanager, logging to /usr/local/hadoop-2.9.2/logs/yarn-root-resourcemanager-d1.out
localhost: starting nodemanager, logging to /usr/local/hadoop-2.9.2/logs/yarn-root-nodemanager-d1.out
[root@d1 hadoop-2.9.2]# jps
4193 ResourceManager
4419 Jps
3732 DataNode
3542 NameNode
4376 NodeManager
4030 SecondaryNameNode
[root@d1 hadoop-2.9.2]#
```

---
layout: post
title: hadoop之伪分布式搭建
---
## 整体流程
hive构建在hadoop之上，要先安装hadoop，然后再安装hive。

## hadoop安装
参照：[hadoop之伪分布式搭建](https://qingzhongli.com/hadoop-pseudo-distributed-operation/)
## 下载hive
download:https://hive.apache.org/downloads.html

## 配置环境变量
```
$ su -
passwd


# tar zxvf apache-hive-3.1.2-bin.tar.gz   
# ls apache-hive-3.1.2-bin
bin  binary-package-licenses  conf  examples  hcatalog  jdbc  lib  LICENSE  NOTICE  RELEASE_NOTES.txt  scripts

# cd apache-hive-3.1.2-bin/
# pwd
/home/liqingzhong/apps/apache-hive-3.1.2-bin
# ~/.bashrc file:

export HIVE_HOME=/home/liqingzhong/apps/apache-hive-3.1.2-bin
export PATH=$PATH:$HIVE_HOME/bin
export CLASSPATH=$CLASSPATH:${HADOOP_HOME}/lib/*:.
export CLASSPATH=$CLASSPATH:${HIVE_HOME}/*:.

$ source ~/.bashrc
```

## 配置hive
```
# cd hadoop-3.1.3/
# pwd
/home/liqingzhong/apps/hadoop-3.1.3
# cd $HIVE_HOME/conf
# cp hive-env.sh.template hive-env.sh
# vi hive-env.sh
# Set HADOOP_HOME to point to a specific hadoop install directory
HADOOP_HOME=/home/liqingzhong/apps/hadoop-3.1.3

# Hive Configuration Directory can be controlled by:
export HIVE_CONF_DIR=/home/liqingzhong/apps/apache-hive-3.1.2-bin/conf
```

## 安装mysql
```
cd mysql/
$ ll
total 519440
-rw-r--r--. 1 root root 531906560 Jan 21  2019 mysql-8.0.13-1.el7.x86_64.rpm-bundle.tar
# tar -xvf mysql-8.0.13-1.el7.x86_64.rpm-bundle.tar
mysql-community-client-8.0.13-1.el7.x86_64.rpm
mysql-community-embedded-compat-8.0.13-1.el7.x86_64.rpm
mysql-community-libs-8.0.13-1.el7.x86_64.rpm
mysql-community-server-8.0.13-1.el7.x86_64.rpm
mysql-community-common-8.0.13-1.el7.x86_64.rpm
mysql-community-devel-8.0.13-1.el7.x86_64.rpm
mysql-community-test-8.0.13-1.el7.x86_64.rpm
mysql-community-libs-compat-8.0.13-1.el7.x86_64.rpm
# rpm -qa|grep mysql
# rpm -ivh  mysql-community-libs-compat-8.0.13-1.el7.x86_64.rpm
warning: mysql-community-libs-compat-8.0.13-1.el7.x86_64.rpm: Header V3 DSA/SHA1 Signature, key ID 5072e1f5: NOKEY
error: Failed dependencies:
        mysql-community-libs(x86-64) >= 8.0.0 is needed by mysql-community-libs-compat-8.0.13-1.el7.x86_64
        mariadb-libs is obsoleted by mysql-community-libs-compat-8.0.13-1.el7.x86_64
# rpm -qa|grep maria
mariadb-libs-5.5.60-1.el7_5.x86_64
# rpm -e --nodeps mariadb-libs-5.5.60-1.el7_5.x86_64
# rpm -ivh mysql-community-libs-8.0.13-1.el7.x86_64.rpm
# rpm -ivh mysql-community-libs-compat-8.0.13-1.el7.x86_64.rpm
# rpm -ivh mysql-community-client-8.0.13-1.el7.x86_64.rpm
# rpm -ivh  mysql-community-server-8.0.13-1.el7.x86_64.rpm
# systemctl status mysqld
● mysqld.service - MySQL Server
   Loaded: loaded (/usr/lib/systemd/system/mysqld.service; enabled; vendor preset: disabled)
   Active: inactive (dead)
     Docs: man:mysqld(8)
           http://dev.mysql.com/doc/refman/en/using-systemd.html
# systemctl start mysqld
# systemctl status mysqld
● mysqld.service - MySQL Server
   Loaded: loaded (/usr/lib/systemd/system/mysqld.service; enabled; vendor preset: disabled)
   Active: active (running) since Tue 2020-04-14 18:59:29 CST; 3s ago
     Docs: man:mysqld(8)
           http://dev.mysql.com/doc/refman/en/using-systemd.html
  Process: 16441 ExecStartPre=/usr/bin/mysqld_pre_systemd (code=exited, status=0/SUCCESS)
 Main PID: 17534 (mysqld)
   Status: "SERVER_OPERATING"
    Tasks: 38
   Memory: 530.7M
   CGroup: /system.slice/mysqld.service
           └─17534 /usr/sbin/mysqld

Apr 14 18:59:02 elk1 systemd[1]: Starting MySQL Server...
Apr 14 18:59:29 elk1 systemd[1]: Started MySQL Server.
# grep "A temporary password" /var/log/mysqld.log
2020-04-14T10:59:16.405875Z 5 [Note] [MY-010454] [Server] A temporary password is generated for root@localhost: F);n?x&u:4Qa
# mysql -u root -p # 以临时密码登录
# mysql -u root -p
Enter password:
Welcome to the MySQL monitor.  Commands end with ; or \g.
Your MySQL connection id is 8
Server version: 8.0.13

Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.

Oracle is a registered trademark of Oracle Corporation and/or its
affiliates. Other names may be trademarks of their respective
owners.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.

mysql> ALTER USER "root"@"localhost" IDENTIFIED  BY "Inspur123!@#"; # mysql 8
Query OK, 0 rows affected (0.06 sec)

mysql> create database hive DEFAULT CHARACTER SET utf8;
CREATE USER 'hive'@'%' IDENTIFIED BY 'Inspur123!@#';
GRANT ALL PRIVILEGES ON hive.* TO 'hive'@'%' WITH GRANT OPTION;Query OK, 1 row affected, 1 warning (0.06 sec)

mysql> CREATE USER 'hive'@'%' IDENTIFIED BY 'Inspur123!@#';
Query OK, 0 rows affected (0.01 sec)

mysql> GRANT ALL PRIVILEGES ON hive.* TO 'hive'@'%' WITH GRANT OPTION;
Query OK, 0 rows affected (0.10 sec)

mysql> quit
#
```

## 初始化元数据
```
# cd $HIVE_HOME/scripts/metastore/upgrade/mysql/
# mysql -u hive -p
Enter password:
Welcome to the MySQL monitor.  Commands end with ; or \g.
Your MySQL connection id is 304
Server version: 8.0.13 MySQL Community Server - GPL

Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.

Oracle is a registered trademark of Oracle Corporation and/or its
affiliates. Other names may be trademarks of their respective
owners.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.

mysql> show tables;
ERROR 1046 (3D000): No database selected
mysql> use hive
Database changed
mysql> source hive-schema-3.1.0.mysql.sql
```
## 注册 Metastore
```
# cd $HIVE_HOME/conf
# cp hive-default.xml.template hive-site.xml
# vi hive-site.xml # 如下节点改为对应配置
<property>
   <name>hive.metastore.db.type</name>
   <value>mysql</value>
   <description>
     Expects one of [derby, oracle, mysql, mssql, postgres].
     Type of database used by the metastore. Information schema &amp; JDBCStorageHandler depend on it.
   </description>
</property>
<property>
   <name>javax.jdo.option.ConnectionURL</name>
   <value>jdbc:mysql://elk1:3306/hive?createDatabaseIfNotExist=true&amp;useSSL=false</value>
   <description>JDBC connect string for a JDBC metastore </description>
</property>
<property>
  <name>javax.jdo.option.ConnectionDriverName</name>
  <value>com.mysql.jdbc.Driver</value>
  <description>Driver class name for a JDBC metastore</description>
</property>
<property>
  <name>javax.jdo.option.ConnectionUserName</name>
  <value>hive</value>
  <description>Username to use against metastore database</description>
</property>
<property>
  <name>javax.jdo.option.ConnectionPassword</name>
  <value>Inspur123!@#</value>
  <description>password to use against metastore database</description>
</property>
```

## 配置mysql驱动
```
# pwd
/home/liqingzhong/apps/apache-hive-3.1.2-bin/lib
# rz
rz waiting to receive.
Starting zmodem transfer.  Press Ctrl+C to cancel.
Transferring mysql-connector-java-5.1.27.jar...
  100%     851 KB     851 KB/sec    00:00:01       0 Errors  

# ls mysql-connector-java-5.1.27.jar
mysql-connector-java-5.1.27.jar
```

## 启动验证

创建hive必须的hdfs目录
```
$ hdfs dfs -mkdir /tmp
$ hdfs dfs -mkdir -p /user/hive/warehouse
$ hdfs dfs -chmod g+w /tmp
$ hdfs dfs -chmod g+w /user/hive/warehouse
```
启动验证
```
# cd $HIVE_HOME
# hive
which: no hbase in (/usr/lib64/qt-3.3/bin:/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin:/home/liqingzhong/apps/hadoop-3.1.3/sbin:/home/liqingzhong/apps/hadoop-3.1.3/bin:/home/liqingzhong/apps/apache-hive-3.1.2-bin/bin:/root/bin)
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/home/liqingzhong/apps/apache-hive-3.1.2-bin/lib/log4j-slf4j-impl-2.10.0.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/home/liqingzhong/apps/hadoop-3.1.3/share/hadoop/common/lib/slf4j-log4j12-1.7.25.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.apache.logging.slf4j.Log4jLoggerFactory]
Hive Session ID = 9dccddb7-fdf4-4573-9b50-32829392e672

Logging initialized using configuration in jar:file:/home/liqingzhong/apps/apache-hive-3.1.2-bin/lib/hive-common-3.1.2.jar!/hive-log4j2.properties Async: true
Hive-on-MR is deprecated in Hive 2 and may not be available in the future versions. Consider using a different execution engine (i.e. spark, tez) or using Hive 1.X releases.
Hive Session ID = 7a88e345-01b4-4f55-93cb-0ebf19867b37
hive> show tables;
OK
Time taken: 0.952 seconds
hive>
```

##  启动hiveserver2
```
# HIVE_HOME/bin/hiveserver2 &
```

## 常见错误
1、使用hive启动时报错，即`java.lang.NoSuchMethodError: com.google.common.base.Preconditions.checkArgument(ZLjava/lang/String;Ljava/lang/Object;)V`
解决方法如下：
```
# ls ../lib/guava-19.0.jar
../lib/guava-19.0.jar
# ls $HADOOP_HOME/share/hadoop/common/lib/guava-27.0-jre.jar  
/home/liqingzhong/apps/hadoop-3.1.3/share/hadoop/common/lib/guava-27.0-jre.jar
# rm ../lib/guava-19.0.jar
# cp $HADOOP_HOME/share/hadoop/common/lib/guava-27.0-jre.jar  ../lib/
```
解决办法参照：[启动hive报错：java.lang.NoSuchMethodError: com.google.common.base.Preconditions.checkArgument(ZLjava/lang/String;Ljava/lang/Object;)V（已解决）](https://qingzhongli.com/hadoop-setup-common-problems)
## References
[Hive - Installation](https://www.tutorialspoint.com/hive/hive_installation.html)

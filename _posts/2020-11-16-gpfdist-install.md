---
layout: post
title: gpfdist服务安装
---

## gpfdist简介

gpfdist是Greenplum数据库并行文件分发程序。可读外部表和gpload使用它为所有Greenplum数据库的segment并行地提供外部表文件。  
可写外部表使用它并行地接受来自Greenplum数据库的segment的输出流，并将它们写到文件中。使用gpfdist的好处是，在读写外部表时，可以保证最大的并行性，从而提供最佳的性能以及更容易管理外部表。更多官方原文描述请进入[gpfdist](https://gpdb.docs.pivotal.io/6-12/utility_guide/ref/gpfdist.html)。

## gpfdist涉及软件下载

要使用gpfdist服务，需要安装greenplum-clients和greenplum-loaders，进入[官网下载地址](https://network.pivotal.io/products/pivotal-gpdb/#/releases)，选择相应版本下载client和loader的两个安装包，本次使用到是4.3.7.0版本，相应文件为：

```bash
greenplum-clients-4.3.7.0-build-2-RHEL5-x86_64.zip
greenplum-loaders-4.3.7.0-build-2-RHEL5-x86_64.zip
```

## gpfdist涉及软件安装

将相应安装包上传并解压，然后先安装greenplum-clients，再安装greenplum-loaders。

### 基础依赖安装

```bash
[root@d6cba3b6f0c5 /]# yum install which java-1.8.0-openjdk -y
```

### greenplum-clients安装

```
[root@d6cba3b6f0c5 /]# ./greenplum-clients-4.3.7.0-build-2-RHEL5-x86_64.bin
```

执行回车后首先会输出使用许可协议，一直回车阅读完后进入正式安装。

出现Do you accept the Pivotal Clients license agreement? \[yes | no\]，在光标处输入yes，然后回车，如下：

```bash
********************************************************************************
    Do you accept the Pivotal Clients license agreement? [yes | no]
********************************************************************************

yes
```

输入yes之后，要确认client的默认安装目录是否为/usr/local/greenplum-clients-4.3.7.0-build-2，如果默认则按回车（enter）进入下一步，如果想要指定目录，可以输入指定的安装目录，此处选择默认安装直接回车（enter）

```bash
********************************************************************************
    Do you accept the Pivotal Clients license agreement? [yes | no]
********************************************************************************

yes

********************************************************************************
Provide the installation path for Greenplum Clients or press ENTER to
accept the default installation path: /usr/local/greenplum-clients-4.3.7.0-build-2
********************************************************************************


```

确认将客户端内容安装到/usr/local/greenplum-clients-4.3.7.0-build-2目录下，输入yes即可进入下一步：

```bash
********************************************************************************
    Do you accept the Pivotal Clients license agreement? [yes | no]
********************************************************************************

yes

********************************************************************************
Provide the installation path for Greenplum Clients or press ENTER to
accept the default installation path: /usr/local/greenplum-clients-4.3.7.0-build-2
********************************************************************************

********************************************************************************
Install Greenplum Clients into </usr/local/greenplum-clients-4.3.7.0-build-2>? [yes | no]
********************************************************************************

yes
```

下面将会检查主机是否存在默认目录，不存在将进行默认目录创建，如下所示：

```bash
********************************************************************************
/usr/local/greenplum-clients-4.3.7.0-build-2 does not exist.
Create /usr/local/greenplum-clients-4.3.7.0-build-2 ? [ yes | no ]
(Selecting no will exit the installer)
********************************************************************************


```

输入yes后，开始创建默认目录，并将文件导入到默认目录中，如果出现Installation complete字样表示client安装完成，默认目录为/usr/local/greenplum-clients-4.3.7.0-build-2，如下：

```bash
********************************************************************************
/usr/local/greenplum-clients-4.3.7.0-build-2 does not exist.
Create /usr/local/greenplum-clients-4.3.7.0-build-2 ? [ yes | no ]
(Selecting no will exit the installer)
********************************************************************************

yes

Extracting product to /usr/local/greenplum-clients-4.3.7.0-build-2


********************************************************************************
Installation complete.
Greenplum Clients is installed in:

  /usr/local/greenplum-clients-4.3.7.0-build-2

Pivotal Greenplum documentation is available
for download at http://docs.gopivotal.com/gpdb
********************************************************************************
[root@d6cba3b6f0c5 /]#
```

到对应目录查看就会看到新创建的目录，如下所示：  

```bash
[root@d6cba3b6f0c5 /]# ls /usr/local                                   
bin  etc  games  greenplum-clients-4.3.7.0-build-2  include  lib  lib64  libexec  sbin  share  src
[root@d6cba3b6f0c5 /]#
```

## greenplum-loaders安装

greenplum-loaders的安装与greenplum-clients的安装步骤基本一致，具体操作过程如下：

```bash
[root@d6cba3b6f0c5 /]# ./greenplum-loaders-4.3.7.0-build-2-RHEL5-x86_64.bin

********************************************************************************
    You must read and accept the Pivotal Loaders license agreement
                             before installing
********************************************************************************

            ***  IMPORTANT INFORMATION - PLEASE READ CAREFULLY  ***

PIVOTAL GREENPLUM DATABASE END USER LICENSE AGREEMENT

......//省略

Rev: Pivotal_GPDB_EULA_03182014.txt


********************************************************************************
    Do you accept the Pivotal Loaders license agreement? [yes | no]
********************************************************************************

yes

********************************************************************************
Provide the installation path for Greenplum Loaders or press ENTER to
accept the default installation path: /usr/local/greenplum-loaders-4.3.7.0-build-2
********************************************************************************



********************************************************************************
Install Greenplum Loaders into </usr/local/greenplum-loaders-4.3.7.0-build-2>? [yes | no]
********************************************************************************

yes

********************************************************************************
/usr/local/greenplum-loaders-4.3.7.0-build-2 does not exist.
Create /usr/local/greenplum-loaders-4.3.7.0-build-2 ? [ yes | no ]
(Selecting no will exit the installer)
********************************************************************************

yes

Extracting product to /usr/local/greenplum-loaders-4.3.7.0-build-2


********************************************************************************
Installation complete.
Greenplum Loaders is installed in:

  /usr/local/greenplum-loaders-4.3.7.0-build-2

Pivotal Greenplum documentation is available
for download at http://docs.gopivotal.com/gpdb
********************************************************************************
[root@d6cba3b6f0c5 /]#
```

## gpfdist使用

首先为使用gpfdist的账户配置环境变量，例如test账户，具体操作如下：

```bash
[test@d6cba3b6f0c5 ~]$ echo 'source /usr/local/greenplum-loaders-4.3.7.0-build-2/greenplum_loaders_path.sh' >> ~/.bashrc
[test@d6cba3b6f0c5 ~]$ echo 'source /usr/local/greenplum-clients-4.3.7.0-build-2/greenplum_clients_path.sh' >> ~/.bashrc
[test@d6cba3b6f0c5 ~]$ source ~/.bashrc
[test@d6cba3b6f0c5 ~]$ echo 'source /usr/local/greenplum-loaders-4.3.7.0-build-2/greenplum_loaders_path.sh' >> ~/.bash_profile
[test@d6cba3b6f0c5 ~]$ echo 'source /usr/local/greenplum-clients-4.3.7.0-build-2/greenplum_clients_path.sh' >> ~/.bash_profile
[test@d6cba3b6f0c5 ~]$ source ~/.bash_profile
[test@d6cba3b6f0c5 ~]$
```

具体用法参照官网[gpfdist](https://gpdb.docs.pivotal.io/6-12/utility_guide/ref/gpfdist.html)的介绍，简单举例如下：

```bash
[test@d6cba3b6f0c5 ~]$ gpfdist -d /data2
2020-11-16 16:56:29 48829 INFO Before opening listening sockets - following listening sockets are available:
2020-11-16 16:56:29 48829 INFO IPV6 socket: [::]:8080
2020-11-16 16:56:29 48829 INFO IPV4 socket: 0.0.0.0:8080
2020-11-16 16:56:29 48829 INFO Trying to open listening socket:
2020-11-16 16:56:29 48829 INFO IPV6 socket: [::]:8080
2020-11-16 16:56:29 48829 INFO Opening listening socket succeeded
2020-11-16 16:56:29 48829 INFO Trying to open listening socket:
2020-11-16 16:56:29 48829 INFO IPV4 socket: 0.0.0.0:8080
Serving HTTP on port 8080, directory /data2
```

## 参考

-   [GP客户端gpfdist部署](https://www.ronpris.com/820.html)
-   [gpfdist](https://gpdb.docs.pivotal.io/6-12/utility_guide/ref/gpfdist.html)
-   [Greenplum Database UNIX Client Documentation](https://gpdb.docs.pivotal.io/5280/client_tool_guides/client-docs-unix.html)

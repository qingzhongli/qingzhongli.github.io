---
layout: post
title: oracle的数据导出方式之SQLULDR2
---
## 简介
SQLULDR2是一个可以从oracle快速导出数据到文件的工具。它可以像插入语句一样自定义导出数据的语句（通过query关键字指定）、自定义字段的分割符、压缩导出文件（通过指定导出文件的后缀，例如：".gz"）、还可以生成sqlldr用的control文件、并行导出、使用密钥对输出文件加密。

## [下载](http://www.onexsoft.com/en/download)安装
```sh
$ unzip sqluldr2linux64.zip
$ mv sqluldr2linux64.bin $ORACLE_HOME/bin/
$ ll $ORACLE_HOME/bin/sqluldr2linux64.bin
-rwxr-xr-x. 1 root root 185894 Jan 27  2016 /opt/oracle/11.2.0/bin/sqluldr2linux64.bin
$ mv $ORACLE_HOME/bin/sqluldr2linux64.bin $ORACLE_HOME/bin/sqluldr2
$ ll $ORACLE_HOME/bin/sqluldr2
-rwxr-xr-x. 1 root root 185894 Jan 27  2016 /opt/oracle/11.2.0/bin/sqluldr2
$ echo 'export LD_LIBRARY_PATH=$ORACLE_HOME/bin:$ORACLE_HOME/lib:/lib:/usr/lib' >> ~/.bash_profile
$ source ~/.bash_profile
$ sqluldr2

SQL*UnLoader: Fast Oracle Text Unloader (GZIP, Parallel), Release 4.0.1
(@) Copyright Lou Fangxin (AnySQL.net) 2004 - 2010, all rights reserved.

License: Free for non-commercial useage, else 100 USD per server.

Usage: SQLULDR2 keyword=value [,keyword=value,...]

Valid Keywords:
   user    = username/password@tnsname
   sql     = SQL file name
   query   = select statement
   field   = separator string between fields
   record  = separator string between records
   rows    = print progress for every given rows (default, 1000000)
   file    = output file name(default: uldrdata.txt)
   log     = log file name, prefix with + to append mode
   fast    = auto tuning the session level parameters(YES)
   text    = output type (MYSQL, CSV, MYSQLINS, ORACLEINS, FORM, SEARCH).
   charset = character set name of the target database.
   ncharset= national character set name of the target database.
   parfile = read command option from parameter file

  for field and record, you can use '0x' to specify hex character code,
  \r=0x0d \n=0x0a |=0x7c ,=0x2c, \t=0x09, :=0x3a, #=0x23, "=0x22 '=0x27

```
## 常见用法

```sh
#!/bin/sh

SQL="select to_char(a.start_time,'yyyy-mm-dd hh24:mi:ss'), \
            a.
            a.city_name \
       from lqz.test partition(p_1d) a
       where a.city_name <> '北京'"

for (( DAY=20181008; DAY<=20181031; DAY++)); do
    PARTITION=p_1d_$DAY
    QUERY=$(echo $SQL | sed s/p_1d/${PARTITION}/g )
    sqluldr user='lqz/123456@192.168.37.10/APP' query="${QUERY}" field=',' file=/data/${PARTITION}.csv
    echo ">>>>>>> ${PARTITION}.csv export finish <<<<<<<"
done

echo ">>>>>>>  all data export finish <<<<<<<"
```
注意：小于1的数值的数据，小数点前的0不显示，例如：0.1导出后变成了.1，可以结合`to_char(数值,'fm9990.0099')`进行转换，规避该问题。

## References

* [SQLULDR2, Unload Rows from Oracle/MySQL to Text File for Data Exchange](http://www.onexsoft.com/en/sqluldr2.html)
* [SQLULDR2 :: Tips 1 – Customize the Field and Record Separators for Flexible Format](http://www.onexsoft.com/en/sqluldr2-field-record-separators.html)
* [【Oracle】oracle sqluldr2工具使用方法](https://www.jianshu.com/p/c23151e1c38c)
* [linux免安装客户端配置-tnsping: error while loading shared libraries: libclntsh.so.11.1: cannot open shared o](https://blog.csdn.net/luopu873/article/details/78912095)

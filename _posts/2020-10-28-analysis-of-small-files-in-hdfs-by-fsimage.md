---
layout: post
title: HDFS中小文件的排查方式之分析fsimage
---
## 问题

cloudera manager监控页面HDFS大部分机器出现类似告警"存在隐患 : DataNode 有 xxxxxx 个块。 警告阈值：500,000 块。"，cm给出的建议：

> 这是 DataNode 运行状况检查，用于检查 DataNode 是否含有过多的块。如果 DataNode 含有过多的块，可能影响 DataNode 的性能。具有大量块数的 DataNode 将需要较大的 java 堆并且可能遇到较长时间的垃圾回收暂停。另外，大量块数可能表明存在许多小文件。不会为处理许多小文件而优化 HDFS，跨许多小文件进行操作时处理时间可能受影响。
>
> 如果只有部分 DataNode 有大量块，运行 HDFS 重新平衡命令可以通过移动 DataNode 之间的数据解决该问题。如果 HDFS 重新平衡命令将群集报告为平衡，没有修复块不平衡，则问题与存在的许多小文件有关。参阅 HDFS 文档了解解决该问题的最佳做法。如果许多小文件不是您的使用案例的关注点，则考虑禁用该运行状况测试。如果所有 DataNode 都有大量块数且该问题与小文件无关，则应添加更多 DataNode。

思路：确认hdfs集群中是否确实存在大量小文件，根据实际需要对小文件进行合并，对于历史数据及时清理归档。

## 排查过程

### 获取fsimage信息

```bash
# Usage: hdfs dfsadmin [-fetchImage <local directory>]
$ hdfs dfsadmin -fetchImage  /data
20/10/27 17:48:04 INFO namenode.TransferFsImage: Opening connection to http://namenode1:50070/imagetransfer?getimage=1&txid=latest
20/10/27 17:48:04 INFO namenode.TransferFsImage: Image Transfer timeout configured to 60000 milliseconds
20/10/27 17:48:05 INFO namenode.TransferFsImage: Transfer took 1.14s at 357022.87 KB/s
$ ll -h /data/fsimage_0000000000930647029
-rw-r----- 1 app app 397M 10月 27 17:48 /data/fsimage_0000000000930647029
```

### 格式化fsimage为可读文本

参照官网给出的《[Offline Image Viewer Guide](https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-hdfs/HdfsImageViewer.html)》，读取fsimage，格式化为文本，具体操作如下：

```bash
$ hdfs oiv -i /data/fsimage_0000000000930647029 -o /data/fsimage.csv -p Delimited  -delimiter ","            
20/10/27 19:35:45 INFO offlineImageViewer.PBImageTextWriter: Loading string table
20/10/27 19:35:45 INFO offlineImageViewer.FSImageHandler: Loading 20 strings
20/10/27 19:35:45 INFO offlineImageViewer.PBImageTextWriter: Loading inode references
20/10/27 19:35:45 INFO offlineImageViewer.FSImageHandler: Loading inode references
20/10/27 19:35:45 INFO offlineImageViewer.FSImageHandler: Loaded 0 inode references
20/10/27 19:35:45 INFO offlineImageViewer.PBImageTextWriter: Loading directories
20/10/27 19:35:45 INFO offlineImageViewer.PBImageTextWriter: Loading directories in INode section.
20/10/27 19:35:52 INFO offlineImageViewer.PBImageTextWriter: Found 224875 directories in INode section.
20/10/27 19:35:52 INFO offlineImageViewer.PBImageTextWriter: Finished loading directories in 6598ms
20/10/27 19:35:52 INFO offlineImageViewer.PBImageTextWriter: Loading INode directory section.
20/10/27 19:35:54 INFO offlineImageViewer.PBImageTextWriter: Scanned 214127 INode directories to build namespace.
20/10/27 19:35:54 INFO offlineImageViewer.PBImageTextWriter: Finished loading INode directory section in 1784ms
20/10/27 19:35:54 INFO offlineImageViewer.PBImageTextWriter: Found 3697297 INodes in the INode section
20/10/27 19:36:55 INFO offlineImageViewer.PBImageTextWriter: Outputted 3697297 INodes.
$ head /data/fsimage.csv
Path,Replication,ModificationTime,AccessTime,PreferredBlockSize,BlocksCount,FileSize,NSQUOTA,DSQUOTA,Permission,UserName,GroupName
/user/hive/warehouse/test.db/table1/partitionday=20200914/partitionhour=16/000151_0.lzo,3,2020-09-15 04:15,2020-09-15 04:15,134217728,1,438472,0,0,-rwxrwxrwt,user1,hive
/user/hive/warehouse/test.db/table1/partitionday=20200914/partitionhour=12/000151_0.lzo,3,2020-09-15 04:15,2020-09-15 04:15,134217728,1,437489,0,0,-rwxrwxrwt,user1,hive
/user/hive/warehouse/test.db/table1/partitionday=20200914/partitionhour=11/000151_0.lzo,3,2020-09-15 04:15,2020-09-15 04:15,134217728,1,437340,0,0,-rwxrwxrwt,user1,hive
/user/hive/warehouse/test.db/table1/partitionday=20200914/partitionhour=09/000151_0.lzo,3,2020-09-15 04:15,2020-09-15 04:15,134217728,1,435482,0,0,-rwxrwxrwt,user1,hive
/user/hive/warehouse/test.db/table1/partitionday=20200914/partitionhour=05/000151_0.lzo,3,2020-09-15 04:15,2020-09-15 04:15,134217728,1,420584,0,0,-rwxrwxrwt,user1,hive
/user/hive/warehouse/test.db/table1/partitionday=20200914/partitionhour=07/000151_0.lzo,3,2020-09-15 04:15,2020-09-15 04:15,134217728,1,432046,0,0,-rwxrwxrwt,user1,hive
/user/hive/warehouse/test.db/table1/partitionday=20200914/partitionhour=19/000151_0.lzo,3,2020-09-15 04:15,2020-09-15 04:15,134217728,1,436986,0,0,-rwxrwxrwt,user1,hive
/user/hive/warehouse/test.db/table1/partitionday=20200914/partitionhour=14/000151_0.lzo,3,2020-09-15 04:15,2020-09-15 04:15,134217728,1,436246,0,0,-rwxrwxrwt,user1,hive
/user/hive/warehouse/test.db/table1/partitionday=20200914/partitionhour=10/000193_0.lzo,3,2020-09-15 04:15,2020-09-15 04:15,134217728,1,443284,0,0,-rwxrwxrwt,user1,hive
$ sed -i -e "1d" /data/fsimage.csv # 删除fsimage.csv的首行表头
```

### 建立存储fsimage的表

```bash
CREATE TABLE `fsimage_info_csv`(
  `path` string,
  `replication` int,
  `modificationtime` string,
  `accesstime` string,
  `preferredblocksize` bigint,
  `blockscount` int,
  `filesize` bigint,
  `nsquota` string,
  `dsquota` string,
  `permission` string,
  `username` string,
  `groupname` string)
ROW FORMAT SERDE
  'org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe'
WITH SERDEPROPERTIES (
  'field.delim'=',',
  'serialization.format'=',')
STORED AS INPUTFORMAT
  'org.apache.hadoop.mapred.TextInputFormat'
OUTPUTFORMAT
  'org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat'
LOCATION
    'hdfs://nameservice1/user/hive/warehouse/fsimage_info_csv';
```

### 加载数据到hive表

```bash
$ hdfs dfs -put /data/fsimage.csv /user/hive/warehouse/fsimage_info_csv/
```

### 查看文件大小分布

根据fsimage文件查看一下文件大小的分布，如下：

```bash
$ hdfs oiv -p FileDistribution  -i fsimage_0000000000930647029 -o fs_distribution                                 
$ cat fs_distribution
Processed 0 inodes.
Processed 1048576 inodes.
Processed 2097152 inodes.
Processed 3145728 inodes.
Size    NumFiles
0       209746
2097152 2360944
4194304 184952
6291456 121774
8388608 37136
// 省略中间部分
10485760        36906
12582912        51616
14680064        19209
16777216        14617
18874368        7655
20971520        5625
23068672        26746
25165824        112429
27262976        10304
29360128        12315
31457280        11966
33554432        15739
35651584        10180
115425148928    1
totalFiles = 3472422
totalDirectories = 224875
totalBlocks = 3401315
totalSpace = 122170845300822
maxFileSize = 115423398874
```

## 逐级目录统计文件数量

以查找三级目录下的小文件数量为例，如下：

```sql
SELECT
    dir_path ,
    COUNT(*) AS small_file_num
FROM
    (    SELECT
            relative_size,
            dir_path
        FROM
            (    SELECT
                    (
                    CASE filesize < 4194304
                        WHEN TRUE
                        THEN 'small'
                        ELSE 'large'
                    END)  AS relative_size,
                    concat('/',split(PATH,'\/')[1], '/',split(PATH,'\/')[2], '/'
                    ,split(PATH,'\/')[3], '/',split(PATH,'\/')[4], '/', split(
                    PATH,'\/')[5]) AS dir_path
                FROM
                    DEFAULT.fsimage_info_csv
                WHERE
                    permission LIKE 'd%') t1
        WHERE
            relative_size='small') t2
GROUP BY
    dir_path
ORDER BY
    small_file_num
```

相应数据脱敏后输出如下：

```
/data/load/201905032130      1
//省略中间部分
/user/hive/warehouse/teset.db/table1  2244
/user/hive/warehouse/teset.db/table2  2244
/user/hive/warehouse/teset.db/table3  2244
/user/hive/warehouse/teset.db/table4  2246
/user/hive/warehouse/teset.db/table5  2246
/user/hive/warehouse/teset.db/table6  2248
/user/hive/warehouse/teset.db/table7  2508
/user/hive/warehouse/teset.db/table8  3427
Time taken: 53.929 seconds, Fetched: 32947 row(s)
```

## 小文件处理

-   根据涉及目录，反向找到涉及程序，尝试优化避免小文件的产生
-   及时合并归档小文件
-   及时清理历史小文件

## 参考

-   [分析hdfs文件变化及监控小文件](https://www.jianshu.com/p/c1c32c4def6f)
-   [hdfs小文件治理方案](https://zhuanlan.zhihu.com/p/211463589)

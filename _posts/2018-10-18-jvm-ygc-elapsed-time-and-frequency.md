---
layout: post
title: jvm之判断ygc耗时和频率
---

首先明确所有分析的java应用的进程编号，然后在根据进程编号获取gc信息和运行时长，最后计算ygc的平均耗时和

**Step1、找出所查java应用的进程编号**

```bash
jps -mlv | grep 应用名称 # 或者 ps -ef | grep 应用名称
```

```bash
20848 com.lqz.test.Main -Dprogram=APP_/home/lqz/test/bin/.. -Xms4G -Xmx4G
```

**Step2、查看应用的gc概况**

```bash
$ jstat -gcutil 20848 250 10
  S0     S1     E      O      P     YGC     YGCT    FGC    FGCT     GCT   
 56.25   0.00  52.10  34.51  39.51 469204 5176.817   549  111.738 5288.555
 56.25   0.00  52.10  34.51  39.51 469204 5176.817   549  111.738 5288.555
 56.25   0.00  52.10  34.51  39.51 469204 5176.817   549  111.738 5288.555
 56.25   0.00  52.10  34.51  39.51 469204 5176.817   549  111.738 5288.555
 56.25   0.00  52.10  34.51  39.51 469204 5176.817   549  111.738 5288.555
 56.25   0.00  52.10  34.51  39.51 469204 5176.817   549  111.738 5288.555
 56.25   0.00  52.10  34.51  39.51 469204 5176.817   549  111.738 5288.555
 56.25   0.00  52.10  34.51  39.51 469204 5176.817   549  111.738 5288.555
 56.25   0.00  54.10  34.51  39.51 469204 5176.817   549  111.738 5288.555
 56.25   0.00  56.11  34.51  39.51 469204 5176.817   549  111.738 5288.555
$ 
```

jstat 详细用法：[jstat - Java Virtual Machine Statistics Monitoring Tool](https://docs.oracle.com/javase/7/docs/technotes/tools/share/jstat.html)，更多的java工具：[JDK Tools and Utilities](https://docs.oracle.com/javase/7/docs/technotes/tools)，更多jdk文档，请进入[传送门](https://docs.oracle.com/en)。

```bash
jstat -<option> [-t] [-h<lines>] <vmid> [<interval> [<count>]
```

```
-gcutil Option
           Summary of Garbage Collection Statistics
    Column      Description
    S0          Survivor space 0 utilization as a percentage of the space's current capacity.
    S1          Survivor space 1 utilization as a percentage of the space's current capacity.
    E           Eden space utilization as a percentage of the space's current capacity.
    O           Old space utilization as a percentage of the space's current capacity.
    P           Permanent space utilization as a percentage of the space's current capacity.
    YGC         Number of young generation GC events.
    YGCT        Young generation garbage collection time.
    FGC         Number of full GC events.
    FGCT        Full garbage collection time.
    GCT         Total garbage collection time.
```

**Step3、查看应用的运行时间**

```bash
$ ps -p 20848 -o etime
    ELAPSED
 7-12:41:04
$ 
```

```bash
ps -p pid -o etime
```

```bash
CODE       HEADER   DESCRIPTION
etime      ELAPSED  elapsed time since the process was started, in the form [[dd-]hh:]mm:ss.
```

**Step4、计算ygc的平均耗时和时间间隔**

ygc平均耗时=YGCT/YGC（s）=5176.81/469204=0.011s=11ms

ygc时间间隔=YGC/程序的运行时间=469204/(7\*24\*60\*60 + 12\*60\*60 + 41\*60 + 4 )=0.72s

> 如果各项参数设置合理，系统没有超时日志出现，GC频率不高，GC耗时不高，那么没有必要进行GC优化；如果GC时间超过1〜3 秒，或者频繁G C ,则必须优化。如果满足下面的指标，则一般不需要进行GC:  
> ■ Minor GC执行时间不到50ms;  
> ■ Minor GC执行不频繁，约10秒一次；  
> ■ Full GC执行时间不到1s;  
> ■ Full GC执行频率不算频繁，不低于10分钟1次。

参考：

-   [《JVM菜鸟进阶高手之路一》](https://mp.weixin.qq.com/s/wRok6M8JJQvMoyBLlTzKYg) 
-   《大话JAVA性能优化》
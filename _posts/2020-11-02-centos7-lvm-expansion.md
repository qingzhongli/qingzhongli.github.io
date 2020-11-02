---
layout: post
title: centos7根目录空间扩容
---

## 1 查看目前分区状况

```bash
[root@test ~]# lsblk
NAME        MAJ:MIN RM   SIZE RO TYPE MOUNTPOINT
sda           8:0    0 446.1G  0 disk
├─sda1        8:1    0     2M  0 part
├─sda2        8:2    0   500M  0 part /boot
└─sda3        8:3    0   132G  0 part
  ├─cl-root 253:0    0    50G  0 lvm  /
  ├─cl-swap 253:1    0    32G  0 lvm  [SWAP]
  └─cl-home 253:2    0    50G  0 lvm  /home
sdb           8:16   0   5.5T  0 disk
└─sdb1        8:17   0   5.5T  0 part /data01
sdc           8:32   0   5.5T  0 disk
└─sdc1        8:33   0   5.5T  0 part /data02
sdd           8:48   0   5.5T  0 disk
└─sdd1        8:49   0   5.5T  0 part /data03
sde           8:64   0   5.5T  0 disk
└─sde1        8:65   0   5.5T  0 part /data04
sdf           8:80   0   5.5T  0 disk
└─sdf1        8:81   0   5.5T  0 part /data05
sdg           8:96   0   5.5T  0 disk
└─sdg1        8:97   0   5.5T  0 part /data06
sdh           8:112  0   5.5T  0 disk
└─sdh1        8:113  0   5.5T  0 part /data07
sdi           8:128  0   5.5T  0 disk
└─sdi1        8:129  0   5.5T  0 part /data08
sdj           8:144  0   5.5T  0 disk
└─sdj1        8:145  0   5.5T  0 part /data09
sdk           8:160  0   5.5T  0 disk
└─sdk1        8:161  0   5.5T  0 part /data10
sdl           8:176  0   5.5T  0 disk
└─sdl1        8:177  0   5.5T  0 part /data11
sdm           8:192  0   5.5T  0 disk
└─sdm1        8:193  0   5.5T  0 part /data12
```

## 2 新增分区，选择primary分区 

```bash
[root@test ~]# fdisk /dev/sda

The device presents a logical sector size that is smaller than
the physical sector size. Aligning to a physical sector (or optimal
I/O) size boundary is recommended, or performance may be impacted.
Welcome to fdisk (util-linux 2.23.2).

Changes will remain in memory only, until you decide to write them.
Be careful before using the write command.


Command (m for help): p

Disk /dev/sda: 479.0 GB, 478998953984 bytes, 935544832 sectors
Units = sectors of 1 * 512 = 512 bytes
Sector size (logical/physical): 512 bytes / 4096 bytes
I/O size (minimum/optimal): 65536 bytes / 65536 bytes
Disk label type: dos
Disk identifier: 0x0003ae25

   Device Boot      Start         End      Blocks   Id  System
/dev/sda1            2048        6143        2048   83  Linux
/dev/sda2   *        6144     1030143      512000   83  Linux
/dev/sda3         1030144   277870591   138420224   8e  Linux LVM

Command (m for help): n
Partition type:
   p   primary (3 primary, 0 extended, 1 free)
   e   extended
Select (default e): p
Selected partition 4
First sector (277870592-935544831, default 277870592):
Using default value 277870592
Last sector, +sectors or +size{K,M,G} (277870592-935544831, default 935544831):
Using default value 935544831
Partition 4 of type Linux and of size 313.6 GiB is set

Command (m for help): p

Disk /dev/sda: 479.0 GB, 478998953984 bytes, 935544832 sectors
Units = sectors of 1 * 512 = 512 bytes
Sector size (logical/physical): 512 bytes / 4096 bytes
I/O size (minimum/optimal): 65536 bytes / 65536 bytes
Disk label type: dos
Disk identifier: 0x0003ae25

   Device Boot      Start         End      Blocks   Id  System
/dev/sda1            2048        6143        2048   83  Linux
/dev/sda2   *        6144     1030143      512000   83  Linux
/dev/sda3         1030144   277870591   138420224   8e  Linux LVM
/dev/sda4       277870592   935544831   328837120   83  Linux

Command (m for help): w
The partition table has been altered!

Calling ioctl() to re-read partition table.

WARNING: Re-reading the partition table failed with error 16: Device or resource busy.
The kernel still uses the old table. The new table will be used at
the next reboot or after you run partprobe(8) or kpartx(8)
Syncing disks.
```

## 3 刷新分区

```bash
[root@test ~]# partprobe
```

## 4 将新分区加入vg中

```bash
[root@test ~]# vgextend cl /dev/sda4
  Physical volume "/dev/sda4" successfully created.
  Volume group "cl" successfully extended
[root@test ~]# vgs
  VG #PV #LV #SN Attr   VSize   VFree  
  cl   2   3   0 wz--n- 445.61g 313.61g
[root@test ~]# lvextend -l +100%FREE /dev/cl/root
  Size of logical volume cl/root changed from 50.00 GiB (12800 extents) to 363.61 GiB (93083 extents).
  Logical volume cl/root successfully resized.
```

## 5 扩展根卷lv

```bash
[root@test ~]# resize
COLUMNS=119;
LINES=33;
export COLUMNS LINES;
[root@test ~]# resize2fs  /dev/cl/root
resize2fs 1.42.9 (28-Dec-2013)
Filesystem at /dev/cl/root is mounted on /; on-line resizing required
old_desc_blocks = 7, new_desc_blocks = 46
The filesystem on /dev/cl/root is now 95316992 blocks long.
```

## 6 扩展后确认

```bash
[root@test ~]# lsblk
NAME        MAJ:MIN RM   SIZE RO TYPE MOUNTPOINT
sda           8:0    0 446.1G  0 disk
├─sda1        8:1    0     2M  0 part
├─sda2        8:2    0   500M  0 part /boot
├─sda3        8:3    0   132G  0 part
│ ├─cl-root 253:0    0 363.6G  0 lvm  /
│ ├─cl-swap 253:1    0    32G  0 lvm  [SWAP]
│ └─cl-home 253:2    0    50G  0 lvm  /home
└─sda4        8:4    0 313.6G  0 part
  └─cl-root 253:0    0 363.6G  0 lvm  /
sdb           8:16   0   5.5T  0 disk
└─sdb1        8:17   0   5.5T  0 part /data01
sdc           8:32   0   5.5T  0 disk
└─sdc1        8:33   0   5.5T  0 part /data02
sdd           8:48   0   5.5T  0 disk
└─sdd1        8:49   0   5.5T  0 part /data03
sde           8:64   0   5.5T  0 disk
└─sde1        8:65   0   5.5T  0 part /data04
sdf           8:80   0   5.5T  0 disk
└─sdf1        8:81   0   5.5T  0 part /data05
sdg           8:96   0   5.5T  0 disk
└─sdg1        8:97   0   5.5T  0 part /data06
sdh           8:112  0   5.5T  0 disk
└─sdh1        8:113  0   5.5T  0 part /data07
sdi           8:128  0   5.5T  0 disk
└─sdi1        8:129  0   5.5T  0 part /data08
sdj           8:144  0   5.5T  0 disk
└─sdj1        8:145  0   5.5T  0 part /data09
sdk           8:160  0   5.5T  0 disk
└─sdk1        8:161  0   5.5T  0 part /data10
sdl           8:176  0   5.5T  0 disk
└─sdl1        8:177  0   5.5T  0 part /data11
sdm           8:192  0   5.5T  0 disk
└─sdm1        8:193  0   5.5T  0 part /data12
[root@test ~]# df -h
Filesystem           Size  Used Avail Use% Mounted on
/dev/mapper/cl-root  358G  7.1G  336G   3% /
devtmpfs             189G     0  189G   0% /dev
tmpfs                189G   84K  189G   1% /dev/shm
tmpfs                189G  155M  188G   1% /run
tmpfs                189G     0  189G   0% /sys/fs/cgroup
/dev/sda2            477M  143M  305M  32% /boot
/dev/sdc1            5.5T   89M  5.2T   1% /data02
/dev/sdl1            5.5T   89M  5.2T   1% /data11
/dev/sdh1            5.5T   89M  5.2T   1% /data07
/dev/sdf1            5.5T   89M  5.2T   1% /data05
/dev/sdi1            5.5T   89M  5.2T   1% /data08
/dev/sdb1            5.5T   89M  5.2T   1% /data01
/dev/sdk1            5.5T   89M  5.2T   1% /data10
/dev/sdd1            5.5T   89M  5.2T   1% /data03
/dev/sdj1            5.5T   89M  5.2T   1% /data09
/dev/sdm1            5.5T   59M  5.2T   1% /data12
/dev/sde1            5.5T   89M  5.2T   1% /data04
/dev/sdg1            5.5T   89M  5.2T   1% /data06
/dev/mapper/cl-home   50G   53M   47G   1% /home
tmpfs                 38G   16K   38G   1% /run/user/42
tmpfs                 38G     0   38G   0% /run/user/0
[root@test ~]#
```

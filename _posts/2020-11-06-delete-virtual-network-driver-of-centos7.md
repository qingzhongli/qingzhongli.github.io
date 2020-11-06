---
layout: post
title: centos7删除虚拟网卡
---

## 背景

cloudera manager页面多台机器提示网络接口速度告警，具体提示为： “存在隐患 : 以下网络接口似乎未以全速运行：virbr0-nic。6 主机网络接口似乎以全速运行。对于 3 主机网络接口，Cloudera Manager Agent 无法确定双工模式或接口速度”

## 查看IP地址 

```
[root@test ~]# ip -4 addr  
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN qlen 1
    inet 127.0.0.1/8 scope host lo
       valid_lft forever preferred_lft forever
8: bond0: <BROADCAST,MULTICAST,MASTER,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP qlen 1000
    inet 10.20.131.37/25 brd 10.20.131.127 scope global bond0
       valid_lft forever preferred_lft forever
9: virbr0: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc noqueue state DOWN qlen 1000
    inet 192.168.122.1/24 brd 192.168.122.255 scope global virbr0
       valid_lft forever preferred_lft foreve

[root@test ~]# ip add
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN qlen 1
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
    inet 127.0.0.1/8 scope host lo
       valid_lft forever preferred_lft forever
    inet6 ::1/128 scope host
       valid_lft forever preferred_lft forever
2: eno1: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc mq state UP qlen 1000
    link/ether ac:1f:6b:c1:95:c8 brd ff:ff:ff:ff:ff:ff
3: eno2: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc mq state DOWN qlen 1000
    link/ether ac:1f:6b:c1:95:c9 brd ff:ff:ff:ff:ff:ff
4: eno3: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc mq state UP qlen 1000
    link/ether ac:1f:6b:c1:95:ca brd ff:ff:ff:ff:ff:ff
5: enp94s0f0: <BROADCAST,MULTICAST,SLAVE,UP,LOWER_UP> mtu 1500 qdisc mq master bond0 state UP qlen 1000
    link/ether 48:f9:7c:ff:3d:df brd ff:ff:ff:ff:ff:ff
6: eno4: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc mq state DOWN qlen 1000
    link/ether ac:1f:6b:c1:95:cb brd ff:ff:ff:ff:ff:ff
7: enp94s0f1: <BROADCAST,MULTICAST,SLAVE,UP,LOWER_UP> mtu 1500 qdisc mq master bond0 state UP qlen 1000
    link/ether 48:f9:7c:ff:3d:df brd ff:ff:ff:ff:ff:ff
8: bond0: <BROADCAST,MULTICAST,MASTER,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP qlen 1000
    link/ether 48:f9:7c:ff:3d:df brd ff:ff:ff:ff:ff:ff
    inet 10.20.131.37/25 brd 10.20.131.127 scope global bond0
       valid_lft forever preferred_lft forever
    inet6 fe80::4af9:7cff:feff:3ddf/64 scope link
       valid_lft forever preferred_lft forever
9: virbr0: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc noqueue state DOWN qlen 1000
    link/ether 52:54:00:d6:51:7e brd ff:ff:ff:ff:ff:ff
    inet 192.168.122.1/24 brd 192.168.122.255 scope global virbr0
       valid_lft forever preferred_lft forever
10: virbr0-nic: <BROADCAST,MULTICAST> mtu 1500 qdisc pfifo_fast master virbr0 state DOWN qlen 1000
    link/ether 52:54:00:d6:51:7e brd ff:ff:ff:ff:ff:ff
[root@test ~]#

```

## 查看网桥

```
[root@test ~]# brctl show
bridge name     bridge id               STP enabled     interfaces
virbr0          8000.525400d6517e       yes             virbr0-nic
[root@test ~]# virsh net-list # Displays a list of defined virtual networks.
 Name                 State      Autostart     Persistent
----------------------------------------------------------
 default              active     yes           yes
[root@test ~]#
```

根据以上输出，发现有一个处于激活状态的以网桥连接的私网地址的virbr0网卡，该网卡是虚拟化涉及的服务libvirtd生成的，本身业务暂不需要，尝试进行关闭，具步骤如下：

## 删除网桥

```
[root@test ~]# ifconfig virbr0 down        # Shut down driver virbr0
[root@test ~]# brctl delbr virbr0          # Delete bridge virbr0
[root@test ~]# ip link set virbr0-nic down # Bring down interface virbr0-nic
[root@test ~]# ip link delete virbr0-nic   # Delete interface virbr0-nic
[root@test ~]# virsh net-destroy default   # Deactivates an active virtual network.
Network default destroyed

[root@test ~]# virsh net-undefine default  # Deletes the persistent libvirt definition of a virtual network.
Network default has been undefined
[root@test ~]#
```

## 禁用libvirtd服务开启启动

```
[root@test ~]# systemctl disable libvirtd.service
Removed symlink /etc/systemd/system/multi-user.target.wants/libvirtd.service.
Removed symlink /etc/systemd/system/sockets.target.wants/virtlockd.socket.
Removed symlink /etc/systemd/system/sockets.target.wants/virtlogd.socket.
[root@test ~]# systemctl mask libvirtd.service        
Created symlink from /etc/systemd/system/libvirtd.service to /dev/null.
[root@test ~]#
```

## 验证虚拟网卡是否删除

```
[root@test ~]# ip -4 add   
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN qlen 1
    inet 127.0.0.1/8 scope host lo
       valid_lft forever preferred_lft forever
8: bond0: <BROADCAST,MULTICAST,MASTER,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP qlen 1000
    inet 10.20.131.37/25 brd 10.20.131.127 scope global bond0
       valid_lft forever preferred_lft forever
[root@test ~]# ip add
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN qlen 1
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
    inet 127.0.0.1/8 scope host lo
       valid_lft forever preferred_lft forever
    inet6 ::1/128 scope host
       valid_lft forever preferred_lft forever
2: eno1: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc mq state UP qlen 1000
    link/ether ac:1f:6b:c1:95:c8 brd ff:ff:ff:ff:ff:ff
3: eno2: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc mq state DOWN qlen 1000
    link/ether ac:1f:6b:c1:95:c9 brd ff:ff:ff:ff:ff:ff
4: eno3: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc mq state UP qlen 1000
    link/ether ac:1f:6b:c1:95:ca brd ff:ff:ff:ff:ff:ff
5: enp94s0f0: <BROADCAST,MULTICAST,SLAVE,UP,LOWER_UP> mtu 1500 qdisc mq master bond0 state UP qlen 1000
    link/ether 48:f9:7c:ff:3d:df brd ff:ff:ff:ff:ff:ff
6: eno4: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc mq state DOWN qlen 1000
    link/ether ac:1f:6b:c1:95:cb brd ff:ff:ff:ff:ff:ff
7: enp94s0f1: <BROADCAST,MULTICAST,SLAVE,UP,LOWER_UP> mtu 1500 qdisc mq master bond0 state UP qlen 1000
    link/ether 48:f9:7c:ff:3d:df brd ff:ff:ff:ff:ff:ff
8: bond0: <BROADCAST,MULTICAST,MASTER,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP qlen 1000
    link/ether 48:f9:7c:ff:3d:df brd ff:ff:ff:ff:ff:ff
    inet 10.20.131.37/25 brd 10.20.131.127 scope global bond0
       valid_lft forever preferred_lft forever
    inet6 fe80::4af9:7cff:feff:3ddf/64 scope link
       valid_lft forever preferred_lft forever
[root@test ~]# brctl show
bridge name     bridge id               STP enabled     interfaces
[root@test ~]# virsh net-list
 Name                 State      Autostart     Persistent
----------------------------------------------------------
[root@test ~]#
```

## 参考

-   [CentOS7.4 删除virbr0虚拟网卡](https://www.cnblogs.com/cloudos/p/8288041.html)
-   [Selected virsh commands](https://www.ibm.com/support/knowledgecenter/en/linuxonibm/com.ibm.linux.z.ldva/ldva_t_virshCommandReference.html)

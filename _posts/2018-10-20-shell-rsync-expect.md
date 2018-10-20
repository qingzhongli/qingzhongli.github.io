---
layout: post
title: shell之使用rsync工具同步文件
---

## 背景
需求是将大量文件上传到远端服务器，若采用ftp协议传输，稳定传输速率慢；若本地可以ssh到远端服务器，可以尝试使用rsync命令直接同步，如果不能免密登录到远端的话，需要结合`expect`来填充用户名密码。本次具体需求是将本地目录`/data/local/ip/day`下的文件回传到对应的`ip`目录下的`day`目录下去。实现方案可以是封装回传脚本结合crontab定期执行，也可以写一个死循环每次回传之后睡一会。

## 结合crontab的具体实现回传的脚本

```bash
#!/bin/bash
#date  :      15:45 2018-10-20
#author:      qingzhongli
#mail  :      qingzhingli2018@gmail.com
#desc  :      rsync local dir to remote dir

#data time
DATA_TIME_DAY=`date  -d "-90 minute" +%Y%m%d`

#current shell name
CUR_SH_NAME=`echo $0 | awk -F'/' '{print $NF}'`

#path that current shell under
CUR_SH_PATH="$(cd `dirname $0`; pwd)/${CUR_SH_NAME}"

#progress count that current shell is executing
P_COUNT=`ps -ef | grep ${CUR_SH_PATH}|grep -v grep | wc -l`

if [ $P_COUNT -gt 3 ];then
    echo "${CUR_SH_PATH} is running. Exit now"
    exit
fi

#rsync source that local machine
RSYNC_LOCAL_DIR='/data/local_dir'

#rsync desntination that remote machine
RSYNC_REMOTE_DIR='/data/remote_dir'

# prefix of rsync remote machine
RSYNC_REMOTE_IP_PREFIX='192.168.37.'

function funRsyncLocalToRemote {
  local IP=$1
  local DAY=$2
  local REMOTE_DIR=$3
  local LOCAL_DIR="${RSYNC_LOCAL_DIR}/${IP}/${DAY}"
  # remote ssh name: test
  # remote ssh password : abc@123
  if [ -d $LOCAL_DIR ];then
      expect -c "
        spawn rsync -avzu --progress --exclude="*.tmp" $LOCAL_DIR test@${IP}:${REMOTE_DIR}
        expect {
                \"*assword\" {set timeout 300; send \"abc\@123\r\";}
                \"yes/no\" {send \"yes\r\"; exp_continue;}
        }
        expect eof"
  fi
}

# rsync local file to remote dir
for IP in `ls ${RSYNC_REMOTE_DIR} | grep ${RSYNC_REMOTE_IP_PREFIX}`
do
    funRsyncLocalToRemote $IP $DATA_TIME_DAY $RSYNC_REMOTE_DIR &
done;
```

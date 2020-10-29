layout: post
title: elasticsearch之索引模板
---
## 索引模板简介

索引模板是创建索引的一种方式。将数据写入指定索引时，如果该索引不存在，则根据索引名称能匹配相应索引模板话，会根据模板的配置建立索引。更多介绍请查看官网的[Index templates](https://www.elastic.co/guide/en/elasticsearch/reference/master/index-templates.html)

## 索引模板查看

查看某个索引模板

```
curl --user ${USERNAME}:${PASSWORD} -XGET "${ES_URL}/_template/ftp_download_log?pretty"
```

查看所有索引模板

```
curl --user ${USERNAME}:${PASSWORD} -XGET "${ES_URL}/_template?pretty"
```

## 索引模板创建

```bash
curl --user ${USERNAME}:${PASSWORD} -XPUT "${ES_URL}/_template/ftp_download_log" -H 'Content-Type: application/json' -d '
{
"index_patterns": ["ftp_download_log_*"],
"settings": {
    "index": {
      "number_of_shards": 12,
      "number_of_replicas": 0,
      "refresh_interval" : "30s"
    }
  },
  "mappings": {
    "properties": {
      "@timestamp": {
        "type": "date"
      },
      "ftpServerName": {
        "type": "keyword"
      },      
      "localPath": {
        "type": "keyword"
      },
      "logType": {
        "type": "keyword"
      },
      "remotePath": {
        "type": "keyword"
      },
      "srcFileSize": {
        "type": "long"
      },
      "srcFileTimestamp": {
        "type": "date"
      },
      "elapsedTime": {
        "type": "long"
      },
      "endTime": {
        "type": "date"
      },
      "startTime": {
        "type": "date"
      }
    }
   }
}'
```

## 索引模板删除

```bash
curl --user ${USERNAME}:${PASSWORD} -XDELETE "${ES_URL}/_template/ftp_download_log"
```

## 索引模板修改

```
curl --user ${USERNAME}:${PASSWORD} -XPUT "${ES_URL}/_template/ftp_download_log" -H 'Content-Type: application/json' -d '
{
"index_patterns": ["ftp_download_log_*"],
"settings": {
    "index": {
      "number_of_shards": 12,
      "number_of_replicas": 0,
      "refresh_interval" : "30s"
    }
  },
  "mappings": {
    "properties": {
      "@timestamp": {
        "type": "date"
      },
      "fileDataTimeMillis": {
        "type": "date"
      },
      "ftpServerName": {
        "type": "keyword"
      }
    }
   }
}'
```

注意：文章中涉及命令是基于elasticsearch 7.1.1版本，与最新版本有较大不同。

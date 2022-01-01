---
layout: post
title: Kafka三种认证模式
---
# Kafka三种认证模式
## 使用kerberos认证
```
bootstrap.servers=hadoop01.com:9092,hadoop02.com:9092,hadoop03.com:9092,hadoop04.com:9092
security.protocol=SASL_PLAINTEXT
sasl.kerberos.service.name=hadoop
# 使用本地缓存
sasl.jaas.config=com.sun.security.auth.module.Krb5LoginModule required useTicketCache=true;.
# 使用keytab模式
#sasl.jaas.config=com.sun.security.auth.module.Krb5LoginModule required useKeyTab=true storekey=true keyTab="/var/lib/key/hadoop.keytab" principal="kafka@HADOOP.COM";
```

## 使用PLAIN认证模式
```
bootstrap.servers=hadoop01.com:9092,hadoop02.com:9092,hadoop03.com:9092,hadoop04.com:9092
security.protocol=SASL_PLAINTEXT
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="kafka" password="kafka";
```

## 使用SSL安全模式
```
bootstrap.servers=hadoop01.com:9091,hadoop02.com:9091,hadoop03.com:9091,hadoop04.com:9091
security.protocol=SSL
ssl.truststore.location=/var/lib/key/hadoop.keystore
ssl.truststore.password=hadoop

ssl.keystore.location=/var/lib/key/hadoop.keystore
ssl.keystore.password=hadoop
ssl.key.password=hadoop

ssl.enabled.protocols=TLsv1.2,TLSv1.1,TLSv1
ssl.truststore.type=JKS
```
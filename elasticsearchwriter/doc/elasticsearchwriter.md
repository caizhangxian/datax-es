# DataX ElasticSearchWriter


---

## 1 快速介绍

数据导入elasticsearch的插件

## 2 实现原理

使用elasticsearch的rest api接口， 批量把从reader读入的数据写入elasticsearch

## 3 功能说明

### 3.1 配置样例

#### job.json

```
{
        "job": {
            "content": [
                {
                    "reader": {
                        "name": "mysqlreader",
                        "parameter": {
                            "column": ["id,isbn"],
                            "connection": [
                                {
                                    "jdbcUrl": ["jdbc:mysql://xxxx/_test?useUnicode=true&characterEncoding=utf8&mysqlEncoding=utf8&allowMultiQueries=true&zeroDateTimeBehavior=convertToNull"],
    				                "querySql": ["SELECT
	s.id,
	s.isbn

FROM
	test s 
"],
                                }
                            ],
    			            "password": "test",
                            "username": "test",
                            "where": "",
    			     "splitPk":"id"
                        }
                    },
                    "writer": {
                        "name": "elasticsearchwriter",
                        "parameter": {
                            "attributeNameSplit": ",",
    			            "attributeNameString": "id,isbn",
                            "batchSize": "1000",
                            "className": "com.alibaba.datax.plugin.writer.elasticsearchwriter.LibraryBooksEntity",
                            "esClusterIP": "ip",
    			                  "esClusterPort": "9200",
                            "esIndex": "indexName",
                            "esType": "doc"
    			           
                        }
                    }
                }
            ],
            "setting": {
                "speed": {
                    "channel": 10
                }
            }
        }
    }

```

#### 3.2 参数说明
* attributeNameString    
    * 要存入的属性名（列名）
    * 必选：是 
    * 默认值：无 
* attributeNameSplit
    * 属性分隔符
    * 必选：是 
    * 默认值：无 
* batchSize
    * 每次写入ES的条数
    * 必选：是 
    * 默认值：无 
* className
    * ES index对应的实体类
    * 必选：是 
    * 默认值：无 
* esClusterIP
    * ES 集群ip
    * 必选：是 
    * 默认值：无 
* esClusterName
    * ES 集群名
    * 必选：是 
    * 默认值：无 
* esClusterPort
    * ES 集群通信端口
    * 必选：否 
    * 默认值：9200
* esIndex
    * ES 索引名
    * 必选：是 
    * 默认值：无 
* esType
    * ES 索引doc_type名
    * 必选：是 
    * 默认值：无 
* urlFieldToParseJson
    * 需要解析为json对象的字段
    * 必选：否
    * 默认值：无 
* timeField
    * 需要在ES mapping中映射为时间的字段
    * 必选：否
    * 默认值：无 
    
    
#### 3.3 ES Mapping设置
由于Mapping设置自由度较高，场景不固定，建议用户自己手动设置ES mapping template，方便日后维护。



## 4 性能报告

### 4.1 环境准备

* 总数据量 1kw条数据, 每条0.1kb
* 1个shard, 0个replica
* 不加id，这样默认是append_only模式，不检查版本，插入速度会有20%左右的提升

#### 4.1.1 输入数据类型(streamreader)

```
{"value": "1.1.1.1", "type": "string"},
{"value": 19890604.0, "type": "double"},
{"value": 19890604, "type": "long"},
{"value": 19890604, "type": "long"},
{"value": "hello world", "type": "string"},
{"value": "hello world", "type": "string"},
{"value": "41.12,-71.34", "type": "string"},
{"value": "2017-05-25", "type": "string"},
```

#### 4.1.2 输出数据类型(eswriter)

```
{ "name": "col_ip","type": "ip" },
{ "name": "col_double","type": "double" },
{ "name": "col_long","type": "long" },
{ "name": "col_integer","type": "integer" },
{ "name": "col_keyword", "type": "keyword" },
{ "name": "col_text", "type": "text"},
{ "name": "col_geo_point", "type": "geo_point" },
{ "name": "col_date", "type": "date"}
```

#### 4.1.2 机器参数

1. cpu: 32  Intel(R) Xeon(R) CPU E5-2650 v2 @ 2.60GHz
2. mem: 128G
3. net: 千兆双网卡

#### 4.1.3 DataX jvm 参数

-Xms1024m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError

### 4.2 测试报告

| 通道数|  批量提交行数| DataX速度(Rec/s)|DataX流量(MB/s)|
|--------|--------| --------|--------|
| 4| 256| 11013| 0.828|
| 4| 1024| 19417| 1.43|
| 4| 4096| 23923| 1.76|
| 4| 8172| 24449| 1.80|
| 8| 256| 21459| 1.58|
| 8| 1024| 37037| 2.72|
| 8| 4096| 45454| 3.34|
| 8| 8172| 45871| 3.37|
| 16| 1024| 67567| 4.96|
| 16| 4096| 78125| 5.74|
| 16| 8172| 77519| 5.69|
| 32| 1024| 94339| 6.93|
| 32| 4096| 96153| 7.06|
| 64| 1024| 91743| 6.74|

### 4.3 测试总结

* 最好的结果是32通道，每次传4096，如果单条数据很大， 请适当减少批量数，防止oom
* 当然这个很容易水平扩展，而且es也是分布式的，多设置几个shard也可以水平扩展

## 5 约束限制

* 如果导入id，这样数据导入失败也会重试，重新导入也仅仅是覆盖，保证数据一致性
* 如果不导入id，就是append_only模式，elasticsearch自动生成id，速度会提升20%左右，但数据无法修复，适合日志型数据(对数据精度要求不高的)

# kafka_consumer_group_exporter

[![Build status](https://travis-ci.org/GuillaumeWaignier/kafka_consumer_group_exporter.svg?branch=master)](https://travis-ci.org/GuillaumeWaignier/kafka_consumer_group_exporter) [![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=org.ianitrix.kafka%3Aconsumer-offset-exportert&metric=alert_status)](https://sonarcloud.io/dashboard/index/org.ianitrix.kafka:consumer-offset-exporter)


JMX exporter for Kafka Consumer Group Offset

## Usage (Command line)

```bash
./bin/consumer-offset-exporter.sh ./config/config.properties
```

The kafka configuration file correspond to the standard kafka [admin client config](https://kafka.apache.org/documentation/#adminclientconfigs).


_Exemple of configuration file_

```properties
bootstrap.servers=localhost:9092
retries=5
```

_Export metric with prometheus_

[JMX exporter](https://github.com/prometheus/jmx_exporter) can be used to export metrics for Prometheus.

```bash
export java_option=-javaagent:/jmx_prometheus_javaagent-0.11.0.jar=8080:/config/prometheus-exporter.yml
./bin/consumer-offset-exporter.sh ./config/config.properties
```

## Usage (Docker)

```bash
docker run -e KAFKAEXPORTER_BOOTSTRAP_SERVERS=kafkaip:9092 -p 8080:8080 ianitrix/kafka-consumer-group-exporter:0.0.1-jre8
```

_Environment variables_

All kafka configuration is done with environment variables prefixed with **KAFKAEXPORTER_**

All dot is replaced by underscore and the variable name must be in upper case.

## Compute lag with prometheus

You need to already collect the log end offset by using the kafka broker metric.

```bash
sum(max(kafka_log_end_offset) by (partition, topic) by (partition, topic) - on (topic, partition) group_right kafka_consumer_offset) by (topic, groupId)
```

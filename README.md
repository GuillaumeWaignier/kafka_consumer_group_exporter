# kafka_consumer_group_exporter
JMX exporter for Kafka Consumer Group Offset

## Usage

```bash
java -jar ConsumerGroupExporter.jar <path_to_kafka_config.properties>
```

The kafka configuration file correspond to the standard kafka [admin client config](https://kafka.apache.org/documentation/#adminclientconfigs).

_Exemple of configuration file_

```properties
bootstrap.servers=localhost:9092
retries=5
```

## Export metric with prometheus

[JMX exporter](https://github.com/prometheus/jmx_exporter) can be used to export metrics for Prometheus.

```bash
java -javaagent:./jmx_prometheus_javaagent-0.11.0.jar=8080:config.yaml -jar ConsumerGroupExporter.jar <path_to_kafka_config.properties>
```


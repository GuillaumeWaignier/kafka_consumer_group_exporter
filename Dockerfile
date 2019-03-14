FROM openjdk:8-jre-alpine

ARG PROMETHEUS_VERSION=0.11.0
ARG CONSUMER_OFFSET_EXPORTER_VERSION=0.0.1

ENV java_option -javaagent:/consumer-offset-exporter/jmx_prometheus_javaagent.jar=8080:/consumer-offset-exporter/config/prometheus-exporter.yml
ENV PATH ${PATH}:/consumer-offset-exporter/bin


ADD https://github.com/GuillaumeWaignier/kafka_consumer_group_exporter/releases/download/v${CONSUMER_OFFSET_EXPORTER_VERSION}/consumer-offset-exporter-${CONSUMER_OFFSET_EXPORTER_VERSION}-bin.tar.gz /

RUN  tar xzf /consumer-offset-exporter-${CONSUMER_OFFSET_EXPORTER_VERSION}-bin.tar.gz \
  && rm /consumer-offset-exporter-${CONSUMER_OFFSET_EXPORTER_VERSION}-bin.tar.gz \
  && ln -s /consumer-offset-exporter-${CONSUMER_OFFSET_EXPORTER_VERSION} /consumer-offset-exporter

# Add JMX exporter for Java
ADD https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/${PROMETHEUS_VERSION}/jmx_prometheus_javaagent-${PROMETHEUS_VERSION}.jar /consumer-offset-exporter/jmx_prometheus_javaagent.jar
RUN chmod 644 /consumer-offset-exporter/jmx_prometheus_javaagent.jar

WORKDIR /consumer-offset-exporter

EXPOSE 8080

ENTRYPOINT ["consumer-offset-exporter.sh"]

# Build
FROM maven:3.6-jdk-8-alpine
COPY . /build
WORKDIR /build
RUN mvn clean package


# RUN
FROM openjdk:8-jre-alpine

ARG PROMETHEUS_VERSION=0.11.0

ENV java_option -javaagent:/consumer-offset-exporter/jmx_prometheus_javaagent.jar=8080:/consumer-offset-exporter/config/prometheus-exporter.yml
ENV PATH ${PATH}:/consumer-offset-exporter/bin

COPY --from=0 /build/target/consumer-offset-exporter-*-bin.tar.gz .

RUN  echo "install consumer-offset-exporter" \
  && tar xzf /consumer-offset-exporter-*-bin.tar.gz \
  && rm /consumer-offset-exporter-*-bin.tar.gz \
  && ln -s /consumer-offset-exporter-* /consumer-offset-exporter \
  && echo "install JMX exporter for Java" \
  && wget -O /consumer-offset-exporter/jmx_prometheus_javaagent.jar https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/${PROMETHEUS_VERSION}/jmx_prometheus_javaagent-${PROMETHEUS_VERSION}.jar \
  && chmod 644 /consumer-offset-exporter/jmx_prometheus_javaagent.jar

WORKDIR /consumer-offset-exporter

EXPOSE 8080

ENTRYPOINT ["consumer-offset-exporter.sh"]

#!/bin/bash
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
docker tag $TRAVIS_COMMIT ianitrix/kafka-consumer-group-exporter:${1}
docker push ianitrix/kafka-consumer-group-exporter:${1}

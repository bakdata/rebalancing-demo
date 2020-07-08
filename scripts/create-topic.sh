#!/bin/bash
source ./scripts/kafka-client.sh

create_topic() {
  releaseName=$1
  zookeeper="$releaseName-cp-zookeeper:2181"
  topicName=$2

  command="kafka-topics --zookeeper $zookeeper --create --topic $topicName --partitions 6 --replication-factor 1"

  echo "Creating topic using '$command'"
  run_command "$command"
}
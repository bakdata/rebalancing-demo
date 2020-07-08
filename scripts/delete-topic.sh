#!/bin/bash
source ./scripts/kafka-client.sh

delete_topic() {
  releaseName=$1
  zookeeper="$releaseName-cp-zookeeper:2181"
  topicName=$2

  command="kafka-topics --zookeeper $zookeeper --delete --topic $topicName"

  echo "Delete topic using '$command'"
  run_command "$command"
}
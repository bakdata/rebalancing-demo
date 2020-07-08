#!/bin/bash
source ./scripts/delete-topic.sh


delete_topic "$1" "input-topic"
delete_topic "$1" "output-topic"
delete_topic "$1" "error-topic"
#!/bin/bash

run_command() {
  kubectl exec -it kafka-client -- /bin/bash -c "$1"
}
"# rebalancing-demo" 

Create clean Kubernetes Cluster (for example with docker-desktop)

### Install [helm](https://helm.sh/docs/intro/install/) - tooling for deploying to kubernetes 

Afterward, install tiller:
```
helm init
```

If `helm init` fails and cannot find the requested resource/tiller please try this instead:
```
helm init --service-account tiller --output yaml | sed 's@apiVersion: extensions/v1beta1@apiVersion: apps/v1@' | sed 's@  replicas: 1@  replicas: 1\n  selector: {"matchLabels": {"app": "helm", "name": "tiller"}}@' | kubectl apply -f -
```

### Install kafka via [confluent helm charts](https://github.com/confluentinc/cp-helm-charts)

```
helm repo add confluentinc https://confluentinc.github.io/cp-helm-charts/
helm repo update
helm install confluentinc/cp-helm-charts --name demo --version 0.4.1
```

This will create a small kafka cluster of 3 brokers and other necessary applications like zookeeper or the schema registry.
This may take a few minutes. Also do not worry of some applications are crashing and restarting at the beginning - that is normal.

### Install our bakdata helm charts

```
helm repo add bakdata-common https://raw.githubusercontent.com/bakdata/common-kafka-streams/master/charts/
```

### Create topics

We you [k9s]() to access the shell of a broker (and more) but you can also connect to the broker via command line:
```
kubectl exec -it demo-cp-kafka-0 -c cp-kafka-broker -- bash
```

In the shell you can create or delete topics.  
Create:
```
kafka-topics --zookeeper demo-cp-zookeeper:2181 --create --topic input-topic --partitions 3 --replication-factor 1
kafka-topics --zookeeper demo-cp-zookeeper:2181 --create --topic error-topic --partitions 3 --replication-factor 1
```

Delete:
```
kafka-topics --zookeeper demo-cp-zookeeper:2181 --delete --topic input-topic
kafka-topics --zookeeper demo-cp-zookeeper:2181 --delete --topic error-topic
```

### Deploying the Application

First we need to build our java application with [jib](https://github.com/GoogleContainerTools/jib):
```
gradlew jibDockerBuild --info --image=demo-consumer
```

You can than upload the consumer to your docker registry. If you do not have one you can create one locally. More info [here](https://docs.docker.com/registry/deploying/).
```
docker run -d -p 5000:5000 --restart=always --name registry registry:2
docker tag demo-consumer:latest localhost:5000/demo-consumer
docker push localhost:5000/demo-consumer
```

Now you can finally deploy the application to our local kubernetes cluster using the `bakdata-common/streams-app` helm chart:
```
helm upgrade --debug --install --recreate-pods --wait --timeout=300 --force --values consumer/values.yaml demo-consumer bakdata-common/streams-app
```
For the test with the `static membership` use:
```
helm upgrade --debug --install --recreate-pods --wait --timeout=300 --force --values consumer/values-static.yaml demo-consumer bakdata-common/streams-app
```
You can always delete the release via `helm delete demo-consumer`.


### Testing the application

This is nice but does basically nothing because we do not send any data to the stream. So back to our shell connected to the kafka broker.  
Kafka added an application called `console-producer` that can send data to a topic. This is exactly the apllication we need:

```
kafka-console-producer --topic input-topic --broker-list localhost:9092
```

To read messages from the output topic use:
```
kafka-console-consumer --topic output-topic --bootstrap-server localhost:9092
```

Now you can write simple text messages to the consumers. There are two special messages:
 * crash - crashes the consumer and the pod restarts
 * block - blocks the consumer for 15 minutes

#### Normal Test

Perquisites:
 * Create topics: `input-topic` and `output-topic`
 * Start non-static deployment
 * Start `kafka-console-producer` and `kafka-console-consumer`
 * Write random strings into the `kafka-console-producer` and see if the messages arrive at the `kafka-console-consumer`. (You will also see them in the logs of the kubernetes pods)
 
Test:
 * Write `wait` into the `kafka-console-producer`. The message blocks one consumer for 15 minutes.
 * Spam some random messages to the `kafka-console-producer`. You should see that some of them do not arrive in the output topic because they are stuck in the blocked consumer.
 * Write `crash` into the `kafka-console-producer`. If no pod restarts please wait a few seconds. It could be that the `crash` messages was sended to the blocked consumer.
 * Spam some random messages to the `kafka-console-producer`.
 
At this point, no messages should arrive at the output because the consumer group is stuck on a rebalance until the blocked consumer finishes processing the `block` message.

#### Static Membership Test

Perquisites:
 * Delete old helm deployment
 * Delete topics: `input-topic` and `output-topic`
 * Create topics: `input-topic` and `output-topic`
 * Start non-static deployment
 * Start `kafka-console-producer` and `kafka-console-consumer`
 * Write random strings into the `kafka-console-producer` and see if the messages arrive at the `kafka-console-consumer`. (This may take a minute now - After this minute al messages will arrive immediately)
 
Test:
 * Write `wait` into the `kafka-console-producer`. One consumer is now blocked for 15 minutes.
 * Spam some random messages to the `kafka-console-producer`. You should see that some of them do not arrive in the output topic because they are stuck in the blocked consumer.
 * Write `crash` into the `kafka-console-producer`. If no pod restarts please wait a few seconds. It could be that the `crash` messages was sended to the blocked consumer.
 * Spam some random messages to the `kafka-console-producer`.
 
Since the consumer group is not rebalancing, the crashing consumer reads the `crash` message again and again and restarts multiple times. 
At this point, roughly a third of all messages should arrive in the output topic. One third arrives at the blocked consumer, and the other third arrive at the crash-looping consumer.
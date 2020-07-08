package com.bakdata.kafka.rebalancingdemo;

import com.bakdata.kafka.Message;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

@Slf4j
public final class SimpleTextProducer {
    public static void main(final String[] args) throws IOException {
        if (args.length != 2) {
            log.info("Please provide command line arguments: configPath topic");
            System.exit(1);
        }

        // Load properties from a local configuration file
        // Create the configuration file (e.g. at '$HOME/.confluent/java.config') with configuration parameters
        // to connect to your Kafka cluster, which can be on your local host, Confluent Cloud, or any other cluster.
        // Follow these detailed instructions to properly create this file: https://github
        // .com/confluentinc/configuration-templates/tree/master/README.md
        final Properties props = loadConfig(args[0]);
        final String topic = args[1];

        // Add additional properties.
        props.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "io.confluent.kafka.serializers.KafkaJsonSerializer");

        try (final Producer<String, Message> producer = new KafkaProducer<>(props)) {

            // Produce sample data
            final long numMessages = 1000L;
            int counter = 0;
            for (long i = 0L; i < numMessages; i++) {
                final String key = "my-key";
                final Message record = buildMessage(counter);
                counter = (counter + 1) % 3;

                log.info("Producing record: {}\t{}", key, record);
                producer.send(new ProducerRecord<>(topic, key, record), (m, e) -> {
                    if (e != null) {
                        log.error("Could not send record", e);
                    } else {
                        log.info("Produced record to topic {} partition [{}] @ offset {}", m.topic(),
                                m.partition(), m.offset());
                    }
                });
            }

            producer.flush();

            log.info("{} messages were produced to topic {}", numMessages, topic);
        }
    }

    private static Message buildMessage(int counter) {
        if (counter == 0) {
            return Message.newBuilder().setContent("simple").build();
        } else if (counter == 1) {
            return Message.newBuilder().setContent("block").build();
        } else {
            return Message.newBuilder().setContent("crash").build();
        }
    }

    public static Properties loadConfig(final String configFile) throws IOException {
        if (!Files.exists(Paths.get(configFile))) {
            throw new IOException(configFile + " not found.");
        }
        final Properties cfg = new Properties();
        try (InputStream inputStream = new FileInputStream(configFile)) {
            cfg.load(inputStream);
        }
        return cfg;
    }
}

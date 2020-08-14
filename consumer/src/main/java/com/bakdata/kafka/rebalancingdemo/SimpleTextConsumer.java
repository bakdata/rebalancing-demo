package com.bakdata.kafka.rebalancingdemo;

import com.bakdata.common_kafka_streams.KafkaStreamsApplication;
import java.time.Duration;
import java.util.Properties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes.StringSerde;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public final class SimpleTextConsumer extends KafkaStreamsApplication {
    private static final long WAIT_MS = Duration.ofMinutes(15).toMillis();

    public static void main(final String[] args) {
        KafkaStreamsApplication.startApplication(new SimpleTextConsumer(), args);
    }

    static String parse(final String input) {
        if ("wait".equalsIgnoreCase(input)) {
            log.info("Block Consumer for " + WAIT_MS + " ms");
            try {
                Thread.sleep(WAIT_MS);
            } catch (InterruptedException e) {
                log.error("Could no wait for " + WAIT_MS + " ms", e);
            }
        } else if ("crash".equalsIgnoreCase(input)) {
            throw new RuntimeException("Application Crashed. Do not know why :(");
        }
        log.info("Forward message: " + input);
        return input;
    }

    @Override
    public void buildTopology(final StreamsBuilder builder) {
        final KStream<String, String> input = builder.stream(this.getInputTopic());

        input
                .mapValues(SimpleTextConsumer::parse)
                .to(this.getOutputTopic());
    }

    @Override
    public Properties createKafkaProperties() {
        final Properties kafkaProperties = super.createKafkaProperties();
        kafkaProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, StringSerde.class.getName());
        kafkaProperties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, StringSerde.class.getName());
        return kafkaProperties;
    }

    @Override
    public String getUniqueAppId() {
        return "demo-simple-text-consumer-" + this.getOutputTopic();
    }
}

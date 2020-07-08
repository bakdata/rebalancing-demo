package com.bakdata.kafka.rebalancingdemo;

import com.bakdata.common_kafka_streams.KafkaStreamsApplication;
import com.bakdata.kafka.ErrorCapturingValueMapper;
import com.bakdata.kafka.ProcessedValue;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
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
    private static final long WAIT_MS = TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES);

    public static void main(final String[] args) {
        KafkaStreamsApplication.startApplication(new SimpleTextConsumer(), args);
    }

    static String parse(final String input) {
        if (input.equals("block")) {
            try {
                Thread.sleep(WAIT_MS);
            } catch (InterruptedException e) {
                log.error("Could no wait for " + WAIT_MS + " ms", e);
            }
        } else if (input.equals("crash")) {
            throw new RuntimeException("Pod crashed");
        }
        return input;
    }

    @Override
    public void buildTopology(final StreamsBuilder builder) {
        final KStream<String, String> input = builder.stream(this.getInputTopics());

        final KStream<String, ProcessedValue<String, String>> processedString = input.mapValues(
                ErrorCapturingValueMapper.captureErrors(SimpleTextConsumer::parse));

        processedString.flatMapValues(ProcessedValue::getValues)
                .to(this.getOutputTopic());

        processedString.flatMapValues(ProcessedValue::getErrors)
                .mapValues(error -> error.createDeadLetter("Could not parse String"))
                .to(this.getErrorTopic());
    }

    @Override
    public Properties createKafkaProperties() {
        final Properties kafkaProperties = super.createKafkaProperties();
        kafkaProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, StringSerde.class);
        return kafkaProperties;
    }

    @Override
    public String getUniqueAppId() {
        return "simple-string-parser-" + this.getOutputTopic();
    }
}

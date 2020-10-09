package com.jesseyates.manning.m1.handler;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.DeserializationExceptionHandler;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;

public class SendToDeadLetterTopicExceptionHandler implements DeserializationExceptionHandler {

    private Logger log = LoggerFactory.getLogger(SendToDeadLetterTopicExceptionHandler.class);
    private KafkaProducer<byte[], byte[]> dlqProducer;
    private String dlqTopic;

    @Override
    public DeserializationHandlerResponse handle(final ProcessorContext context,
                                                 final ConsumerRecord<byte[], byte[]> record,
                                                 final Exception exception) {

        log.warn("Exception caught during Deserialization, sending to the dead queue topic; " +
                        "taskId: {}, topic: {}, partition: {}, offset: {}",
                context.taskId(), record.topic(), record.partition(), record.offset(),
                exception);

        dlqProducer.send(new ProducerRecord<>(dlqTopic, null, record.timestamp(), record.key(), record.value()));

        return DeserializationHandlerResponse.CONTINUE;
    }

    @Override
    public void configure(final Map<String, ?> configs) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, configs.get(StreamsConfig.APPLICATION_ID_CONFIG));
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, configs.get(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG));
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, configs.get(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        dlqProducer = new KafkaProducer<>(props);
        dlqTopic = (String) configs.get("dead-letter-topic");
    }
}

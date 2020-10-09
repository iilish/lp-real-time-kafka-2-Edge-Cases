package com.jesseyates.manning.m1.stream.slow;

import com.jesseyates.manning.common.App;
import com.jesseyates.manning.common.StreamProcessor;
import com.jesseyates.manning.m1.handler.SendToDeadLetterTopicExceptionHandler;
import com.jesseyates.manning.m1.stream.StreamTopology;
import com.jesseyates.manning.m1.stream.S3Client;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import manning.devices.raw.m1.CanonicalRecord;
import manning.devices.raw.m1.RawRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

import java.util.Properties;

public class SlowMessageStream extends App<SlowMessageConf> {

    @Override
    public void initialize(Bootstrap<SlowMessageConf> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );

        bootstrap.addCommand(new SlowMessageProcessor("slow",
                "Runs a Kafka stream application"));
    }

    public static void main(String[] args) throws Exception {
        new SlowMessageStream().run(args);
    }

    /**
     * Stream Processor that takes slow {@link RawRecord} from S3 and convert it to {@link CanonicalRecord}
     */
    class SlowMessageProcessor extends StreamProcessor<SlowMessageConf> {

        protected SlowMessageProcessor(String name, String description) {
            super(name, description);
        }

        @Override
        protected KafkaStreams buildStream(SlowMessageConf conf, Properties props, SchemaRegistryClient client) {
            if (client == null) {
                client = new CachedSchemaRegistryClient(conf.getKafka().getSchemaRegistry(), 10);
            }

            S3Client s3Client = new S3Client(S3Client.newClient(conf.getS3Key(), conf.getS3Secret()));
            final Serde<RawRecord> valueSpecificAvroSerde = new SpecificAvroSerde<>(client);
            configureAvroSerde(conf, valueSpecificAvroSerde, false);
            Topology deviceStateTopology = StreamTopology.buildSlowTopology(s3Client, conf, valueSpecificAvroSerde);

            props.put("dead-letter-topic", conf.getDeadLetter());
            props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
                    SendToDeadLetterTopicExceptionHandler.class.getName()
            );

            return new KafkaStreams(deviceStateTopology, props);
        }

    }
}
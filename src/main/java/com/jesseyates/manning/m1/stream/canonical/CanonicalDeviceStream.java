package com.jesseyates.manning.m1.stream.canonical;

import com.jesseyates.manning.common.App;
import com.jesseyates.manning.common.StreamProcessor;
import com.jesseyates.manning.m1.handler.SendToDeadLetterTopicExceptionHandler;
import com.jesseyates.manning.m1.stream.StreamTopology;
import com.jesseyates.manning.m1.stream.state.DeviceStateStreamConf;
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

public class CanonicalDeviceStream extends App<DeviceStateStreamConf> {

    @Override
    public void initialize(Bootstrap<DeviceStateStreamConf> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );

        bootstrap.addCommand(new CanonicalDeviceProcessor("canonical", "Runs a Kafka stream application"));
    }

    public static void main(String[] args) throws Exception {
        new CanonicalDeviceStream().run(args);
    }

    /**
     * Stream Processor that takes {@link RawRecord} and convert it to {@link CanonicalRecord}
     */
    class CanonicalDeviceProcessor extends StreamProcessor<CanonicalDeviceConf> {

        protected CanonicalDeviceProcessor(String name, String description) {
            super(name, description);
        }

        @Override
        protected KafkaStreams buildStream(CanonicalDeviceConf conf, Properties props, SchemaRegistryClient client) {
            if (client == null) {
                client = new CachedSchemaRegistryClient(conf.getKafka().getSchemaRegistry(), 10);
            }

            final Serde<RawRecord> valueSpecificAvroSerde = new SpecificAvroSerde<>(client);
            configureAvroSerde(conf, valueSpecificAvroSerde, false);
            Topology deviceStateTopology = StreamTopology.buildCanonicalTopology(conf, valueSpecificAvroSerde);

            props.put("dead-letter-topic", conf.getDeadLetter());
            props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
                    SendToDeadLetterTopicExceptionHandler.class.getName()
            );

            return new KafkaStreams(deviceStateTopology, props);
        }

    }
}


/*

import com.manning.energy.battery.application.api.DeviceStateEvent;
import com.manning.energy.battery.application.jdbi.DeviceStateDAO;
import com.manning.energy.battery.generated.DeviceEventRow;
import com.manning.energy.battery.util.KafkaUtil;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class IngestTopology {
    static private Logger logger = Logger.getLogger(DeviceStateStream.class);

    public static Topology buildStateTopology(final DeviceStateDAO dao, String topicIn, String topicOut,
                                 SchemaRegistryClient schemaRegistryClient,
                                 String schemaRegistryUrl) {
        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, DeviceEventRow> EnergyEventStream = builder
                .stream(topicIn, Consumed.with(Serdes.String(),
                        getAvroSerde(schemaRegistryClient, schemaRegistryUrl)));

        KStream<String, DeviceStateEvent> transformedStream = EnergyEventStream
                .mapValues(
                        (key, value) -> {
                            logger.info(" inserting into DB " + key);

                            final DeviceStateEvent state = new DeviceStateEvent(value);
                            dao.createOrUpdate(state.getDeviceId(), state.getCharging());
                            return state;
                        }
                );

        Serde<DeviceStateEvent> serdeJson = KafkaUtil.jsonSerde();
        transformedStream.to(topicOut, Produced.with(Serdes.String(), serdeJson));

        return builder.buildStateTopology();

    }

    static <T extends SpecificRecord> SpecificAvroSerde<T> getAvroSerde(SchemaRegistryClient client, String url) {
        final SpecificAvroSerde<T> avroSerde = new SpecificAvroSerde<>(client);
        Map<String, String> serdeConfig = new HashMap<>();
        serdeConfig.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, url);
        avroSerde.configure(serdeConfig, false);
        return avroSerde;
    }


}

 */
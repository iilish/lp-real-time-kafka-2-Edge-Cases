package com.jesseyates.manning.common;

import com.jesseyates.manning.m1.handler.SendToDeadLetterTopicExceptionHandler;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static org.apache.kafka.streams.StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG;

/**
 * Base class for stream processors.
 */
public abstract class StreamProcessor<T extends StreamConfiguration>
  extends ConfiguredCommand<T> {

  protected StreamProcessor(String name, String description) {
    super(name, description);
  }

  @Override
  protected void run(Bootstrap<T> bootstrap, Namespace namespace, T configuration)
    throws Exception {
    Properties props = bootstrapProperties(configuration);
    // https://kafka.apache.org/10/documentation/streams/developer-guide/datatypes.html#streams-developer-guide-serdes
    props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, getKeySerde());
    props.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, getValueSerde());

    KafkaStreams streams = buildStream(configuration, props, null);
    Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

    streams.start();
  }

  public static <T extends StreamConfiguration> Properties bootstrapProperties(T configuration){
    Properties props = new Properties();
    // Set a few key parameters
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, configuration.getApplicationId());
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
      configuration.getKafka().getBootstrapServers());
    props.put("schema.registry.url", configuration.getKafka().getSchemaRegistry());

    // By default, we assume that records that are 'bad' cause the stream to just fail - logging
    // the exception and then exiting. If its a temporary problem in the processing, then when the
    // stream gets picked up by another instance (e.g. a restarted instance in Kubernetes), it
    // will get retried. If its a fatal error, then we expect that the stream will block
    // indefinitely. That means alerting would notify the user that they have a problem b/c the
    // processing is not making any forward progress.
    props.put(
      StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
            LogAndFailExceptionHandler.class.getName()
    );

    props.putAll(configuration.getKafka().getOverrides());
    return props;
  }

  public static <T extends GenericRecord> Serde<T> configureAvroSerde(StreamConfiguration conf,
    Serde<T> avro, boolean isKey){
    final Map<String, String> serdeConfig = Collections.singletonMap("schema.registry.url",
      conf.getKafka().getSchemaRegistry());
    avro.configure(serdeConfig, isKey);
    return avro;
  }

  protected Class<? extends Serde> getKeySerde() {
    return Serdes.String().getClass();
  }

  protected Class<? extends Serde> getValueSerde() {
    return SpecificAvroSerde.class;
  }

  protected abstract KafkaStreams buildStream(T configuration, Properties props, SchemaRegistryClient client);
}

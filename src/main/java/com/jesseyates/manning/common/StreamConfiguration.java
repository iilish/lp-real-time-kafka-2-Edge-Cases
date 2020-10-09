package com.jesseyates.manning.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;

public class StreamConfiguration extends Configuration {

  @NotNull
  private String applicationId;
  private KafkaStream kafka = new KafkaStream();
  private String source;


  @JsonProperty("applicationId")
  public String getApplicationId() {
    return applicationId;
  }

  @JsonProperty("applicationId")
  public void setApplicationId(String ApplicationId) {
    applicationId = ApplicationId;
  }

  @JsonProperty("kafka")
  public KafkaStream getKafka() {
    return kafka;
  }

  @JsonProperty("kafka")
  public void setKafka(KafkaStream Kafka) {
    kafka = Kafka;
  }

  public static class KafkaStream {
    @NotNull
    private String schemaRegistry;
    @NotNull
    private String bootstrapServers;
    @NotNull
    private Map<String, String> overrides = Collections.emptyMap();

    @JsonProperty("schema-registry-url")
    public String getSchemaRegistry() {
      return schemaRegistry;
    }

    @JsonProperty("schema-registry-url")
    public void setSchemaRegistry(String SchemaRegistry) {
      schemaRegistry = SchemaRegistry;
    }

    @JsonProperty("bootstrap-servers")
    public String getBootstrapServers() {
      return bootstrapServers;
    }

    @JsonProperty("bootstrap-servers")
    public void setBootstrapServers(String BootstrapServer) {
      bootstrapServers = BootstrapServer;
    }

    @JsonProperty("overrides")
    public Map<String, String> getOverrides() {
      return overrides;
    }

    @JsonProperty("overrides")
    public void setOverrides(Map<String, String> Overrides) {
      overrides = Overrides;
    }
  }

  @JsonProperty("source")
  public String getSource() {
    return source;
  }

  @JsonProperty("source")
  public void setSource(String Source) {
    source = Source;
  }


}

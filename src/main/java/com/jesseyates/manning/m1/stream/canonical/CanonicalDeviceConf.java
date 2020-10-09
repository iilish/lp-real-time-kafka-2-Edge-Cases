package com.jesseyates.manning.m1.stream.canonical;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jesseyates.manning.common.StreamConfiguration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class CanonicalDeviceConf extends StreamConfiguration {

  @Valid
  @NotNull
  private String deadLetter;
  private String slowMessage;
  private String canonicalTopic;

  private Integer messageThreshold;

  @JsonProperty("dead")
  public String getDeadLetter() {
    return deadLetter;
  }

  @JsonProperty("dead")
  public void setDeadLetter(String deadLetter) {
    this.deadLetter = deadLetter;
  }

  @JsonProperty("slow")
  public String getSlowTopic() {
    return slowMessage;
  }

  @JsonProperty("slow")
  public void setSlowMessage(String slowMessage) {
    this.slowMessage = slowMessage;
  }

  @JsonProperty("canonical")
  public String getCanonicalTopic() {
    return canonicalTopic;
  }

  @JsonProperty("canonical")
  public void setCanonicalTopic(String canonicalTopic) {
    this.canonicalTopic = canonicalTopic;
  }

  @JsonProperty("message-threshold")
  public Integer getMessageThreshold() {
    return messageThreshold;
  }

  @JsonProperty("message-threshold")
  public void setMessageThreshold(Integer messageThreshold) {
    this.messageThreshold = messageThreshold;
  }
}

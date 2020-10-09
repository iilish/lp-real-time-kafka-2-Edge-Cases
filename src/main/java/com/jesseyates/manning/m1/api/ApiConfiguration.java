package com.jesseyates.manning.m1.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class ApiConfiguration extends Configuration {

    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();
    private String deviceTable;
    private String s3Key;
    private String s3Secret;
    private long threshold;

    @NotNull
    private String topic;
    @NotNull
    private Map<String, String> kafka = new HashMap<>();

    @JsonProperty("database")
    public void setDataSourceFactory(DataSourceFactory factory) {
        this.database = factory;
    }

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    @JsonProperty("topic")
    public String getTopic() {
        return topic;
    }

    @JsonProperty("topic")
    public void setTopic(String Topic) {
        topic = Topic;
    }

    @JsonProperty("kafka")
    public Map<String, String> getKafka() {
        return this.kafka;
    }

    @JsonProperty("kafka")
    public void setKafka(Map<String, String> kafka) {
        this.kafka = kafka;
    }

    @JsonProperty("deviceTable")
    public String getDeviceTable() {
        return deviceTable;
    }

    @JsonProperty("deviceTable")
    public void setDeviceTable(String deviceTable) {
        this.deviceTable = deviceTable;
    }

    @JsonProperty("s3-key")
    public String getS3Key() {
        return s3Key;
    }

    @JsonProperty("s3-key")
    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    @JsonProperty("s3-secret")
    public String getS3Secret() {
        return s3Secret;
    }

    @JsonProperty("s3-secret")
    public void setS3Secret(String s3Secret) {
        this.s3Secret = s3Secret;
    }

    @JsonProperty("size-threshold")
    public Long getThreshold() {
        return threshold;
    }

    @JsonProperty("size-threshold")
    public void setThreshold(Long threshold) {
        this.threshold = threshold;
    }
}

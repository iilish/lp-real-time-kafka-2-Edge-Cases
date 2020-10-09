package com.jesseyates.manning.m1.stream.slow;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jesseyates.manning.common.StreamConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class SlowMessageConf extends StreamConfiguration {

    @Valid
    @NotNull
    private String deadLetter;
    private String canonicalTopic;
    private String s3Key;
    private String s3Secret;


    @JsonProperty("dead")
    public String getDeadLetter() {
        return deadLetter;
    }

    @JsonProperty("dead")
    public void setDeadLetter(String deadLetter) {
        this.deadLetter = deadLetter;
    }

    @JsonProperty("canonical")
    public String getCanonicalTopic() {
        return canonicalTopic;
    }

    @JsonProperty("canonical")
    public void setCanonicalTopic(String canonicalTopic) {
        this.canonicalTopic = canonicalTopic;
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
}

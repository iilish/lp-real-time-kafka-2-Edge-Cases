package com.jesseyates.manning.m1.stream;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.StringInputStream;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class S3Client {
    private final AmazonS3 client;
    private final String defaultBucket;
    private static final String defaultRegion = "eu-west-3";

    public S3Client(AmazonS3 client) {
        this(client, "manning-kafka-bucket");
    }

    public S3Client(AmazonS3 client, String defaultBucket) {
        this.client = client;
        this.defaultBucket = defaultBucket;
    }

    public static AmazonS3 newClient(String key, String secret) {
        Region currentRegion = Regions.getCurrentRegion();

        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(key, secret)))
                .withRegion(currentRegion == null ? defaultRegion : currentRegion.getName())
                .build();
    }

    public PutObjectResult putString(String path, String content) throws IOException {

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.getBytes(StandardCharsets.UTF_8).length);

        return client.putObject(defaultBucket, path, new StringInputStream(content), metadata);
    }

    public String putObject(String path, byte[] content) {
        InputStream input = new ByteArrayInputStream(content);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.length);

        client.putObject(defaultBucket, path, input, metadata);

        return path;
    }

    public void retrieveObject(String path, String filePath) {
        File file = new File(filePath);
        client.getObject(new GetObjectRequest(defaultBucket, path), file);
    }

    public S3Object getObject(String path) {
        return client.getObject(new GetObjectRequest(defaultBucket, path));
    }

    public Boolean doesObjectExist(String path) {
        return client.doesObjectExist(defaultBucket, path);
    }
    
}

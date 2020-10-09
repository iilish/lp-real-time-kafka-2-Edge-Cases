package com.jesseyates.manning.m1.api;

import com.google.common.collect.ImmutableMap;
import com.jesseyates.manning.m1.stream.S3Client;
import manning.devices.raw.m1.RawRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.apache.commons.io.IOUtils.toByteArray;

/**
 * API endpoint for collecting devices
 */
@Path("/")
public class DeviceEndpoint {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final KafkaProducer producer;
    private final DeviceDAO db;
    private final String table;
    private final S3Client client;
    protected final String topic;
    private final Long threshold;

    public DeviceEndpoint(KafkaProducer producer, String topic,
                          S3Client client, Long threshold,
                          DeviceDAO db, String table) {
        this.producer = producer;
        this.topic = topic;
        this.db = db;
        this.table = table;
        this.threshold = threshold;
        this.client = client;
    }

    /**
     * Sends the payload to Kafka as a message, keyed on the device UUID. This ensures that devices
     * always land in the same partition, in order, giving us a consistent historical view of the
     * device over time.
     *
     * @return the {@link RecordMetadata} for the record sent to Kafka. This is more for debugging,
     * in production you wouldn't want to waste bandwidth sending this data since devices in the
     * field do not have access to the backend instance/
     */
    @POST
    @Path("/send/{uuid}")
    @Consumes({APPLICATION_OCTET_STREAM, APPLICATION_JSON})
    @Produces(APPLICATION_JSON)
    public Response send(@PathParam("uuid") String uuid, @Context HttpServletRequest request)
            throws ExecutionException, InterruptedException, IOException {
        byte[] input = toByteArray(request.getInputStream());
        int inputLength = input.length;

        RawRecord.Builder payload = RawRecord.newBuilder()
                .setUuid(uuid)
                .setArrivalTimeMs(Instant.now().toEpochMilli());

        logger.info("receiving message of " + inputLength  + " bytes");
        if (isBigRecord(inputLength)) {
            String pathReference = client.putObject(getPath(uuid), input);
            payload.setReference(pathReference);
            logger.info("A message with uuid " + uuid + " is going to be stored in "+pathReference);
        } else {
            payload.setBody(ByteBuffer.wrap(input));
        }

        ProducerRecord<String, RawRecord> record = new ProducerRecord<>(topic, uuid, payload.build());
        Future<RecordMetadata> metadata = producer.send(record);


        return Response.ok().entity(serialize(metadata.get())).build();
    }

    private String getPath(String uuid) {
        LocalDate date = LocalDate.now();
        String today = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return today + "/" + uuid;
    }

    private boolean isBigRecord(int bodyLength) {
        return bodyLength > this.threshold;
    }

    protected Map<String, Object> serialize(RecordMetadata metadata) {
        return ImmutableMap.<String, Object>builder()
                .put("offset", metadata.offset())
                .put("partition", metadata.partition())
                .put("topic", metadata.topic())
                .put("timestamp", metadata.timestamp())
                .build();
    }

    @GET
    @Path("/state")
    public Response getStatus(@QueryParam("uuid") String uuid) {
        return Response.ok().entity(db.getDeviceState(table, uuid)).build();
    }
}

package com.jesseyates.manning.m1.stream;

import com.amazonaws.services.s3.model.S3Object;
import com.jesseyates.manning.m1.api.DeviceDAO;
import com.jesseyates.manning.m1.stream.canonical.CanonicalDeviceConf;
import com.jesseyates.manning.m1.stream.slow.SlowMessageConf;
import com.jesseyates.manning.m1.stream.state.DeviceStateStreamConf;
import manning.devices.raw.m1.CanonicalRecord;
import manning.devices.raw.m1.DeviceRecord;
import manning.devices.raw.m1.RawRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Predicate;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.io.IOUtils.toByteArray;

public class StreamTopology {
    private static Logger logger  = LoggerFactory.getLogger(StreamTopology.class);

    public static Topology buildStateTopology(DeviceDAO db, String table,
                                              DeviceStateStreamConf conf, Serde<CanonicalRecord> valueSpecificAvroSerde) {


        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, CanonicalRecord> energyEventStream = builder
                .stream(conf.getSource(), Consumed.with(Serdes.String(), valueSpecificAvroSerde));

        energyEventStream.mapValues(CanonicalRecord::getBody)
                .foreach((uuid, canonicalRecord) -> {
                    logger.info("state topo: reading uuid " + uuid);
                    long charging = canonicalRecord.getCharging();
                    db.createOrUpdate(table, uuid, charging > 0);
                });

        /*
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
*/
        return builder.build();

    }

    public static Topology buildCanonicalTopology(CanonicalDeviceConf conf, Serde<RawRecord> valueSpecificAvroSerde) {

        BytesToJson toJson = new BytesToJson();

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, RawRecord> rawEventStream = builder
                .stream(conf.getSource(), Consumed.with(Serdes.String(), valueSpecificAvroSerde));

        Predicate<String, RawRecord> isSmallMessage = (key, rawRecord) ->
                rawRecord.getReference() == null && rawRecord.getBody() != null;
        Predicate<String, RawRecord> isBigMessage = (key, rawRecord) ->
                rawRecord.getReference() != null && rawRecord.getBody() == null;

        // small messages
        KStream<String, RawRecord> onlySmallSizeEventStream = rawEventStream.filter(isSmallMessage);

        onlySmallSizeEventStream
                .mapValues(rawRecord -> getStringObjectMap(rawRecord.getUuid().toString(),
                        rawRecord.getArrivalTimeMs(),
                        rawRecord.getBody())
                )
                .flatMapValues(StreamTopology.toCanonical)
                .to(conf.getCanonicalTopic());

        // big messages
        KStream<String, RawRecord> onlyBigSizeEventStream = rawEventStream.filter(isBigMessage);
        onlyBigSizeEventStream.to(conf.getSlowTopic());

        return builder.build();
    }

    public static Topology buildSlowTopology(S3Client client, SlowMessageConf conf, Serde<RawRecord> valueSpecificAvroSerde) {
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, RawRecord> rawEventStream = builder
                .stream(conf.getSource(), Consumed.with(Serdes.String(), valueSpecificAvroSerde));
        rawEventStream
                .mapValues(rawRecord -> {
                    S3Object s3Obj = client.getObject(rawRecord.getReference().toString());
                    try {
                        ByteBuffer body = ByteBuffer.wrap(toByteArray(s3Obj.getObjectContent()));
                        return getStringObjectMap(rawRecord.getUuid().toString(), rawRecord.getArrivalTimeMs(), body);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .flatMapValues(StreamTopology.toCanonical)
                .to(conf.getCanonicalTopic());

        return builder.build();
    }

    private static Map<String, Object> getStringObjectMap(String uuid, long arrivalTime, ByteBuffer body) {
        BytesToJson toJson = new BytesToJson();

        Map<String, Object> map = new HashMap<>();
        map.put("uuid", uuid);
        map.put("arrival_time_ms", arrivalTime);
        map.put("body", toJson.apply(body));
        return map;
    }

    private static DeviceRecord.Builder toDeviceRecord(Map<String, Object> bodyMap) {
        Optional<Integer> charging = Optional.ofNullable(bodyMap.get("charging")).map(value -> (Integer) value);
        Optional<String> chargingSource = Optional.ofNullable(bodyMap.get("charging_source")).map(value -> (String) value);
        Optional<Integer> currentCapacity = Optional.ofNullable(bodyMap.get("current_capacity")).map(value -> (Integer) value);
        Optional<Integer> inverterState = Optional.ofNullable(bodyMap.get("inverter_state")).map(value -> (Integer) value);
        Optional<Double> socRegulator = Optional.ofNullable(bodyMap.get("SoC_regulator")).map(value -> (Double) value);

        return DeviceRecord.newBuilder()
                .setCharging(charging.orElse(-99))
                .setChargingSource(chargingSource.orElse(null))
                .setCurrentCapacity(currentCapacity.orElse(null))
                .setInverterState(inverterState.orElse(null))
                .setSoCRegulator(socRegulator.orElse(null));
    }

    private static ValueMapper<Map<String, Object>, List<CanonicalRecord>> toCanonical = (rawMap) -> {

        String uuid = (String) rawMap.get("uuid");
        Optional<Long> arrivalTime = Optional.ofNullable(rawMap.get("arrival_time_ms")).map(value -> (Long) value);

        List<Map<String, Object>> body = (List<Map<String, Object>>) rawMap.get("body");
        return body.stream()
                .map(bodyMap -> toDeviceRecord(bodyMap).setUuid(uuid).build())
                .map(deviceRecord -> CanonicalRecord.newBuilder()
                        .setBody(deviceRecord)
                        .setArrivalTimeMs(arrivalTime.orElse(null))
                        .build()
                ).collect(Collectors.toList());
    };
}


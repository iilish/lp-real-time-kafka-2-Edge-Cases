package com.jesseyates.manning.m1.api;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;

public interface DeviceDAO {

    @SqlQuery("SELECT state FROM <table> WHERE UUID = :uuid")
    boolean getDeviceState(@Define("table") String table, @Bind("uuid") String uuid);

    @SqlUpdate("INSERT INTO <table> (UUID, STATE) VALUES (:uuid, :state)")
    void setDeviceState(@Define("table") String table,
                        @Bind("uuid") String uuid, @Bind("state") boolean state);

    @SqlUpdate("UPDATE <table> SET STATE = :state WHERE UUID = :uuid")
    void updateDeviceState(@Define("table") String table,
                           @Bind("uuid") String uuid, @Bind("state") boolean state);

    @SqlQuery("SELECT UUID from <table> where uuid = :uuid")
    Optional<String> findById(@Define("table") String table, @Bind("uuid") String uuid);

    default String createOrUpdate(String table, String uuid, boolean state) {
        if (!findById(table, uuid).isPresent()) {
            setDeviceState(table, uuid, state);
        } else {
            updateDeviceState(table, uuid, state);
        }
        return findById(table, uuid).get();
    }
}

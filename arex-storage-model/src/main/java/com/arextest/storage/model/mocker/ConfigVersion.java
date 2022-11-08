package com.arextest.storage.model.mocker;

/**
 * The application depend any config resources when the running,
 * we should be mocked and restore before start replay.
 * This interface mark the mocker as a config resource type.
 * we used to build a batch No. and fetch the config resource content.
 *
 * @author jmo
 * @since 2021/11/17
 */
public interface ConfigVersion {
    String getAppId();

    Integer getRecordVersion();

    /**
     * @return The name of config,eg: file name
     */
    default String getKey() {
        return null;
    }
}
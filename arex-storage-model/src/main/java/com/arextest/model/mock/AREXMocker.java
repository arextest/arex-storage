package com.arextest.model.mock;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@FieldNameConstants
public class AREXMocker implements Mocker {

    /**
     * 1、Only for editing dependencies,the entry point ignored
     * 2、During query, record the id of the mock, and use the id to associate data during comparison
     */
    private String id;
    /**
     * the value required and empty not allowed
     */
    private MockCategoryType categoryType;
    private String replayId;
    private String recordId;
    private String appId;
    private int recordEnvironment;
    /**
     * millis from utc format without timezone
     */
    private long creationTime;
    private long updateTime;
    private long expirationTime;
    private Target targetRequest;
    private Target targetResponse;
    /**
     * the value required and empty allowed
     * for example: pattern of servlet web api
     */
    private String operationName;
    /**
     * record the version of recorded data
     */
    private String recordVersion;
    private Integer continuousFailCount;
    public AREXMocker() {

    }
    public AREXMocker(MockCategoryType categoryType) {
        this.categoryType = categoryType;
    }
}
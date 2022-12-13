package com.arextest.model.mock;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AREXMocker implements Mocker {

    public AREXMocker() {

    }

    public AREXMocker(MockCategoryType categoryType) {
        this.categoryType = categoryType;
    }

    /**
     * Only for editing dependencies,the entry point ignored
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
    private Target targetRequest;
    private Target targetResponse;
    /**
     * the value required and empty allowed
     * for example: pattern of servlet web api
     */
    private String operationName;
    private String configBatchNo;
}
package com.arextest.storage.model.dao;

import lombok.Data;

@Data
public class ServiceOperationEntity extends BaseEntity {
    private String appId;
    private String serviceId;
    private String operationName;
    /**
     * {@link com.arextest.storage.model.enums.MockCategoryType}.codeValue
     */
    private int operationType;
    /**
     * REPLAY = 1
     * RECORD = 2
     * Normal = 4
     */
    private int status;
}

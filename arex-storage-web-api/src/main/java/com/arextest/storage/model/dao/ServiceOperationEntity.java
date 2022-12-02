package com.arextest.storage.model.dao;

import com.arextest.model.mock.MockCategoryType;
import lombok.Data;

@Data
public class ServiceOperationEntity extends BaseEntity {
    private String appId;
    private String serviceId;
    private String operationName;
    /**
     * {@link MockCategoryType}
     */
    private String operationType;
    /**
     * REPLAY = 1
     * RECORD = 2
     * Normal = 4
     */
    private int status;
}
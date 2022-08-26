package com.arextest.storage.model.dao;

import lombok.Data;

@Data
public class ServiceOperationEntity extends BaseEntity {
    private String appId;
    private String serviceId;
    private String operationName;
    private Integer operationType;
    private Integer status;
}

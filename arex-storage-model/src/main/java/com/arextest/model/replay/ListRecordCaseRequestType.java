package com.arextest.model.replay;

import lombok.Data;

@Data
public class ListRecordCaseRequestType {
    private String appId;
    private String operationName;
    private String operationType;
    private Integer pageSize;
    private String lastId;
}

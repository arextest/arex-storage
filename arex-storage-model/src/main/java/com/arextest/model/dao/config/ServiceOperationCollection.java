package com.arextest.model.dao.config;

import java.util.Set;

import com.arextest.model.dao.BaseEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;

@Data
@NoArgsConstructor
@FieldNameConstants
public class ServiceOperationCollection extends BaseEntity {

    public static final String DOCUMENT_NAME = "ServiceOperation";

    @NonNull
    private String appId;
    @NonNull
    private String serviceId;
    @NonNull
    private String operationName;
    // operation response used to convert to schema
    private String operationResponse;
    @Deprecated
    private String operationType;
    private Set<String> operationTypes;
    @NonNull
    private Integer recordedCaseCount;
    @NonNull
    private Integer status;
}

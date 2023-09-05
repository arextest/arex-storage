package com.arextest.model.dao.config;

import com.arextest.model.dao.BaseEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;

@Data
@NoArgsConstructor
@FieldNameConstants
public class ServiceCollection extends BaseEntity {

    public static final String DOCUMENT_NAME = "Service";

    @NonNull
    private String appId;
    @NonNull
    private String serviceName;
    @NonNull
    private String serviceKey;
    @NonNull
    private Integer status;

}

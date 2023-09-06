package com.arextest.config.model.dao.config;

import com.arextest.config.model.dao.BaseEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;


@Data
@NoArgsConstructor
@FieldNameConstants
public class DynamicClassCollection extends BaseEntity {

    public static final String DOCUMENT_NAME = "DynamicClass";

    @NonNull
    private String appId;

    private String fullClassName;

    private String methodName;

    private String parameterTypes;

    private int configType;
}

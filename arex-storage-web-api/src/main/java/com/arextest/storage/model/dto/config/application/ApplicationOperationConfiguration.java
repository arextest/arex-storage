package com.arextest.storage.model.dto.config.application;

import java.util.Set;

import com.arextest.storage.model.dto.config.AbstractConfiguration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationOperationConfiguration extends AbstractConfiguration implements OperationDescription {
    private String id;
    private String appId;
    private String serviceId;
    private String operationName;
    @Deprecated
    private String operationType;
    private Set<String> operationTypes;
}

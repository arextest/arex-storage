package com.arextest.storage.model.dto.config.application;


import java.util.List;

import com.arextest.storage.model.dto.config.AbstractConfiguration;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ApplicationServiceConfiguration extends AbstractConfiguration implements ServiceDescription {
    private String id;
    private String appId;
    private String serviceName;
    private String serviceKey;
    private List<ApplicationOperationConfiguration> operationList;
}
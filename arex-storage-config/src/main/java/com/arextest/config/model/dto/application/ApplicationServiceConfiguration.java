package com.arextest.config.model.dto.application;


import com.arextest.config.model.dto.AbstractConfiguration;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class ApplicationServiceConfiguration extends AbstractConfiguration implements ServiceDescription {
    private String id;
    private String appId;
    private String serviceName;
    private String serviceKey;
    private List<ApplicationOperationConfiguration> operationList;
}
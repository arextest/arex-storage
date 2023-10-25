package com.arextest.config.model.dto.application;

import com.arextest.config.model.dto.AbstractConfiguration;
import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * created by xinyuan_wang on 2023/3/14
 */
@Data
public class InstancesConfiguration extends AbstractConfiguration {
    private String id;
    private String appId;
    private String recordVersion;
    private String host;
    private Date dataUpdateTime;
    private String agentStatus;
    private Map<String, String> systemEnv;
    private Map<String, String> systemProperties;
}

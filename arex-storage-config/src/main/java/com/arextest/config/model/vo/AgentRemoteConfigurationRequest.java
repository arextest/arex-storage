package com.arextest.config.model.vo;

import java.util.Map;
import lombok.Data;

/**
 * @author b_yu
 * @since 2023/6/14
 */
@Data
public class AgentRemoteConfigurationRequest {

  private String appId;
  private String host;
  private String recordVersion;
  private String agentStatus;
  private Integer status;
  private Map<String, String> systemEnv;
  private Map<String, String> systemProperties;
}

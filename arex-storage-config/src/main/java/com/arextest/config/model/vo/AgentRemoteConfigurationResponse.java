package com.arextest.config.model.vo;

import com.arextest.config.model.dto.record.DynamicClassConfiguration;
import com.arextest.config.model.dto.record.ServiceCollectConfiguration;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * @author b_yu
 * @since 2023/6/14
 */
@Data
public class AgentRemoteConfigurationResponse {

  private ServiceCollectConfiguration serviceCollectConfiguration;
  private List<DynamicClassConfiguration> dynamicClassConfigurationList;

  /**
   * Bit flag composed of bits that record/replay are enabled. see
   * {@link com.arextest.config.model.dto.StatusType}
   */
  private Integer status;

  private String targetAddress;
  /**
   * Description of agent startup
   */
  private String message;
  /**
   * Determine whether to start the agent
   */
  private Boolean agentEnabled;

  private Map<String, String> extendField;

  private CompareConfiguration compareConfiguration;
}
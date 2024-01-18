package com.arextest.config.model.dto;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractMultiEnvConfiguration<T>
    extends AbstractConfiguration implements MultiEnvConfig<T> {

  /**
   * Multi environment configuration
   */
  private List<T> multiEnvConfigs;

  /**
   * Multi environment tags
   */
  private Map<String, String> envTags;
}

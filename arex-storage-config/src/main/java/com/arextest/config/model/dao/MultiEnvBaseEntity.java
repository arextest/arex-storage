package com.arextest.config.model.dao;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Setter
@Getter
@FieldNameConstants
public abstract class MultiEnvBaseEntity<T> extends BaseEntity {
  /**
   * Multi environment configuration
   */
  private List<T> multiEnvConfigs;

  /**
   * Multi environment tags
   */
  private Map<String, String> envTags;
}

package com.arextest.config.model.vo;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * @author wildeslam.
 * @create 2023/9/15 14:21
 */
@Data
public class AddApplicationRequest {

  @NotNull
  private String appName;
  @NotEmpty
  private Set<String> owners;
  /**
   * @see com.arextest.model.replay.AppVisibilityLevelEnum .
   */
  private int visibilityLevel;
}

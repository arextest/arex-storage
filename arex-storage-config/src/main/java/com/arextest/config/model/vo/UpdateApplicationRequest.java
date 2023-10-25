package com.arextest.config.model.vo;

import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author wildeslam.
 * @create 2023/9/19 11:21
 */
@Data
public class UpdateApplicationRequest {

  @NotNull
  private String appId;
  private String appName;
  private Set<String> owners;
}

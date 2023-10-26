package com.arextest.config.model.vo;

import java.util.Set;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

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
}

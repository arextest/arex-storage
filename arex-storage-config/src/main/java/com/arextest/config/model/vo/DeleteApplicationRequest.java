package com.arextest.config.model.vo;

import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author wildeslam.
 * @create 2023/12/14 16:26
 */
@Data
public class DeleteApplicationRequest {
  @NotNull
  private String appId;
}

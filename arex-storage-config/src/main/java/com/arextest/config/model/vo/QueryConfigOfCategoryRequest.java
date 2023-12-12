package com.arextest.config.model.vo;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * created by xinyuan_wang on 2023/12/4
 */
@Data
public class QueryConfigOfCategoryRequest {

  @NotNull
  private String appId;

  private String operationName;

  private String categoryName;

  @NotEmpty
  private Boolean entryPoint;
}
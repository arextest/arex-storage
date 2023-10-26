package com.arextest.config.model.dto.application;

import lombok.Data;

@Data
public class Dependency {

  private String dependencyId;
  private String operationName;
  private String operationType;
}
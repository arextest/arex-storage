package com.arextest.config.model.dto.application;

import com.arextest.config.model.dto.AbstractConfiguration;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@FieldNameConstants
public class ApplicationOperationConfiguration extends AbstractConfiguration implements
    OperationDescription {

  private String id;
  private String appId;
  private String serviceId;
  private String operationName;
  @Deprecated
  private String operationType;
  private Set<String> operationTypes;
  @Deprecated
  private String operationResponse;
  private Integer recordedCaseCount;
  private List<Dependency> dependencyList;
}

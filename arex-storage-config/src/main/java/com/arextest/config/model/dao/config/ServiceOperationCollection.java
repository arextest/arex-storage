package com.arextest.config.model.dao.config;

import com.arextest.config.model.dao.BaseEntity;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@FieldNameConstants
@Document(ServiceOperationCollection.DOCUMENT_NAME)
public class ServiceOperationCollection extends BaseEntity {

  public static final String DOCUMENT_NAME = "ServiceOperation";

  @NonNull
  private String appId;
  @NonNull
  private String serviceId;
  @NonNull
  private String operationName;
  // operation response used to convert to schema
  private String operationResponse;
  @Deprecated
  private String operationType;
  private Set<String> operationTypes;
  @NonNull
  private Integer recordedCaseCount;
  @NonNull
  private Integer status;
}

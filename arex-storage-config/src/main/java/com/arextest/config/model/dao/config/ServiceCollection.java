package com.arextest.config.model.dao.config;

import com.arextest.config.model.dao.BaseEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@FieldNameConstants
@Document(ServiceCollection.DOCUMENT_NAME)
public class ServiceCollection extends BaseEntity {

  public static final String DOCUMENT_NAME = "Service";

  @NonNull
  private String appId;
  @NonNull
  private String serviceName;
  @NonNull
  private String serviceKey;
  @NonNull
  private Integer status;

}

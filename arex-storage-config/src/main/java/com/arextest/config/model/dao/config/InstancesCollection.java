package com.arextest.config.model.dao.config;

import com.arextest.config.model.dao.BaseEntity;
import java.util.Date;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;

@Data
@NoArgsConstructor
@FieldNameConstants
public class InstancesCollection extends BaseEntity {

  public static final String DOCUMENT_NAME = "Instances";

  @NonNull
  private String appId;
  private String host;
  private String recordVersion;
  private Date dataUpdateTime;
  private String agentStatus;
  private Map<String, String> systemEnv;
  private Map<String, String> systemProperties;
}

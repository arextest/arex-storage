package com.arextest.config.model.dao.config;

import com.arextest.config.model.dao.BaseEntity;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;

import java.util.Set;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@FieldNameConstants
@Document(AppCollection.DOCUMENT_NAME)
public class AppCollection extends BaseEntity {

  public static final String DOCUMENT_NAME = "App";

  @NonNull
  private String appId;

  private int features;

  @NonNull
  private String groupName;
  @NonNull
  private String groupId;

  private String agentVersion;

  private String agentExtVersion;
  @NonNull
  private String appName;

  private String description;
  @NonNull
  private String category;
  private String owner;
  private Set<String> owners;
  @NonNull
  private String organizationName;
  @NonNull
  private Integer recordedCaseCount;
  @NonNull
  private String organizationId;
  @NonNull
  private Integer status;

  private int visibilityLevel;
  private Map<String, Set<String>> tags;
}

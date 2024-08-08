package com.arextest.model.scenepool;

import java.util.Date;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@FieldNameConstants
@Document
public class Scene {
  @Id
  private String id;
  private String sceneKey;
  private String appId;
  private String recordId;

  private String executionPath;

  private Date creationTime;
  private Date updateTime;
  private Date expirationTime;
}

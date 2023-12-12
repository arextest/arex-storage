package com.arextest.model.scenepool;

import java.util.Date;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants
public class Scene {
  private String sceneKey;
  private String appId;
  private String recordId;

  private String executionPath;

  private Date creationTime;
  private Date updateTime;
  private Date expirationTime;
}

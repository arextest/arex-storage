package com.arextest.config.model.dto.application;

import lombok.Data;

/**
 * @author wildeslam.
 * @create 2024/7/3 17:08
 */

@Data
public class AppContract {

  private String id;
  private String appId;
  private Integer contractType;
  private String operationId;

  /**
   * the operationName and operationType exist, when contractType is 2
   */
  private String operationName;
  private String operationType;
  private String contract;
}

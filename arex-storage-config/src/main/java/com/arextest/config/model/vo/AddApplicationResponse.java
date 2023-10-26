package com.arextest.config.model.vo;

import lombok.Data;

/**
 * @author wildeslam.
 * @create 2023/9/15 14:31
 */
@Data
public class AddApplicationResponse {

  private Boolean success;
  private String msg;
  private String appId;
}

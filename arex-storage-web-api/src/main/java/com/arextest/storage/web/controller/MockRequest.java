package com.arextest.storage.web.controller;

import lombok.Data;

@Data
public class MockRequest {

  private String appId;
  private String operationName;
  private String recordId;
  private int type;
}

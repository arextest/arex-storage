package com.arextest.storage.web.controller;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class MockRequest {
    private String appId;
    private String operationName;
    private String recordId;
    private int type;
}

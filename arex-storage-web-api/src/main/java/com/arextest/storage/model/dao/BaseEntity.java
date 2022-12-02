package com.arextest.storage.model.dao;

import lombok.Data;

@Data
public class BaseEntity {
    private String id;
    private long dataChangeCreateTime;
    private long dataChangeUpdateTime;
}
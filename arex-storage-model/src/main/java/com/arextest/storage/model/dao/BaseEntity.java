package com.arextest.storage.model.dao;

import lombok.Data;
import org.bson.types.ObjectId;

@Data
public class BaseEntity {
    private ObjectId id;
    private long dataChangeCreateTime;
    private long dataChangeUpdateTime;
}

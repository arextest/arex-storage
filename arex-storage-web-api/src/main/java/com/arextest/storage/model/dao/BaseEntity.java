package com.arextest.storage.model.dao;

import lombok.Data;
import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

@Data
public class BaseEntity {
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;
    private long dataChangeCreateTime;
    private long dataChangeUpdateTime;
}
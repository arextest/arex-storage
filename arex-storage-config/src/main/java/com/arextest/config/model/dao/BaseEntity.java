package com.arextest.config.model.dao;

import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants
public class BaseEntity {
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;
    private Long dataChangeCreateTime;
    private Long dataChangeUpdateTime;
}
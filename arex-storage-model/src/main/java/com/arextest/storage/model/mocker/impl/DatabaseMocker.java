package com.arextest.storage.model.mocker.impl;

import com.arextest.storage.model.mocker.AbstractMocker;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

/**
 * @author yongwuhe
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DatabaseMocker extends AbstractMocker {
    private String dbName;
    private String parameters;
    private String sql;
    private String keyHolder;


    @BsonId
    @Override
    public ObjectId getId() {
        return super.getId();
    }
}
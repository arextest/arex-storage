package com.arextest.storage.model.mocker.impl;


import com.arextest.storage.model.mocker.AbstractMocker;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

/**
 * @author jmo
 * @since 2021/11/2
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DalResultMocker extends AbstractMocker {
    private String database;
    private String sql;
    private String parameter;
    private String typeName;
    private String keyHolder;
    private String methodName;


    @BsonId
    @Override
    public ObjectId getId() {
        return super.getId();
    }
}
package com.arextest.storage.model.mocker.impl;

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
public class SoaExternalMocker extends ServiceMocker {

    @BsonId
    @Override
    public ObjectId getId() {
        return super.getId();
    }

}
package com.arextest.storage.model.mocker.impl;

import com.arextest.storage.model.annotations.FieldCompression;
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
public class HttpClientMocker extends AbstractMocker {
    private String contentType;
    private String url;
    @FieldCompression
    private String request;
    private String method;

    @BsonId
    @Override
    public ObjectId getId() {
        return super.getId();
    }
}
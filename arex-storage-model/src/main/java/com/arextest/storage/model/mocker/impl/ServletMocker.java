package com.arextest.storage.model.mocker.impl;

import com.arextest.storage.model.annotations.FieldCompression;
import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.AbstractMocker;
import com.arextest.storage.model.mocker.MainEntry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.codecs.pojo.annotations.BsonIgnore;

/**
 * @author yongwuhe
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ServletMocker extends AbstractMocker implements MainEntry {
    private String method;
    private String path;
    private String pattern;
    private String requestHeaders;
    private String responseHeaders;
    @FieldCompression
    private String request;

    private int env;

    @JsonIgnore
    @BsonIgnore
    @Override
    public int getCategoryType() {
        return MockCategoryType.SERVLET_ENTRANCE.getCodeValue();
    }

    @Override
    public void setEnv(int env) {
        this.env = env;
    }

}
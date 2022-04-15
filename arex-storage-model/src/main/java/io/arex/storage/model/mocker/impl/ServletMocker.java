package io.arex.storage.model.mocker.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.arex.storage.model.annotations.FieldCompression;
import io.arex.storage.model.enums.MockCategoryType;
import io.arex.storage.model.mocker.AbstractMocker;
import io.arex.storage.model.mocker.MainEntry;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.util.Map;

/**
 * @author yongwuhe
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ServletMocker extends AbstractMocker implements MainEntry {
    private String method;
    private String path;
    private String pattern;
    private Map<String, String> requestHeaders;
    private Map<String, String> responseHeaders;
    @FieldCompression
    private String request;
    @FieldCompression
    private String response;
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

    @BsonId
    @Override
    public String getRecordId() {
        return super.getRecordId();
    }
}

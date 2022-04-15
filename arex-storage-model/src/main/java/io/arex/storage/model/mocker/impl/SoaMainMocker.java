package io.arex.storage.model.mocker.impl;

import io.arex.storage.model.enums.MockCategoryType;
import io.arex.storage.model.mocker.MainEntry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;

/**
 * @author jmo
 * @since 2021/11/2
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SoaMainMocker extends ServiceMocker implements MainEntry {
    /**
     * 结果类型字符串
     */
    private String responseType;
    private Integer configVersion;
    private String agentVersion;
    private String format;
    private int env;

    @JsonIgnore
    @BsonIgnore
    @Override
    public int getCategoryType() {
        return MockCategoryType.SOA_MAIN.getCodeValue();
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

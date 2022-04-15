package io.arex.storage.model.mocker.impl;

import io.arex.storage.model.enums.MockCategoryType;
import io.arex.storage.model.mocker.MainEntry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.codecs.pojo.annotations.BsonId;

/**
 * @author jmo
 * @since 2021/11/2
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QmqConsumerMocker extends MessageMocker implements MainEntry {
    private int env;
    private String consumerGroupName;

    @Override
    public void setEnv(int env) {
        this.env = env;
    }

    @Override
    public String getConsumerGroupName() {
        return this.consumerGroupName;
    }

    @BsonId
    @Override
    public String getRecordId() {
        return super.getRecordId();
    }

    @JsonIgnore
    @Override
    public String getRequest() {
        return this.getMsgBody();
    }


    @JsonIgnore
    @Override
    public int getCategoryType() {
        return MockCategoryType.QMQ_CONSUMER.getCodeValue();
    }
}

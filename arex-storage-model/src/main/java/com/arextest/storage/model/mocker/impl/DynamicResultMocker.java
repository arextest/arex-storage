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
public class DynamicResultMocker extends AbstractMocker {
    private String clazzName;
    private String operation;
    private String operationKey;

    @Deprecated
    public String getOperationResult() {
        return super.getResponse();
    }

    @Deprecated
    public void setOperationResult(String operationResult) {
        super.setResponse(operationResult);
    }

    @Deprecated
    public String getResultClazz() {
        return super.getResponseType();
    }

    @Deprecated
    public void setResultClazz(String resultClazz) {
        super.setResponseType(resultClazz);
    }


    @BsonId
    @Override
    public ObjectId getId() {
        return super.getId();
    }
}
package com.arextest.storage.model.mocker.impl;


import com.arextest.storage.model.mocker.AbstractMocker;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author jmo
 * @since 2021/11/2
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RedisMocker extends AbstractMocker {
    private String clusterName;
    private String redisKey;

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

}
package com.arextest.storage.model.mocker.impl;

import com.arextest.storage.model.annotations.FieldCompression;
import com.arextest.storage.model.mocker.AbstractMocker;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
}
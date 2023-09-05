package com.arextest.storage.model.dto.config.application;


import java.util.Set;

/**
 * @author jmo
 * @since 2021/12/21
 */
public interface OperationDescription {
    String getOperationName();

    @Deprecated
    String getOperationType();

    Set<String> getOperationTypes();
}

package com.arextest.config.model.dto.application;

import java.util.List;

/**
 * @author jmo
 * @since 2021/12/21
 */
public interface ServiceDescription {
    String getAppId();

    String getServiceName();

    String getServiceKey();

    List<? extends OperationDescription> getOperationList();
}

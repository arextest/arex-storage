package com.arextest.storage.service.config.provider;

import com.arextest.config.model.dto.application.ServiceDescription;

import java.util.List;

/**
 * @author jmo
 * @since 2022/1/21
 */
public interface ApplicationServiceDescriptionProvider {
    List<? extends ServiceDescription> get(String appId);
}

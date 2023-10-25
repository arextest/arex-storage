package com.arextest.storage.service.config.provider;

import com.arextest.config.model.dto.application.ApplicationDescription;

/**
 * The basic application info provider,eg:appName,owner,
 *
 * @author jmo
 * @since 2022/1/21
 */
public interface ApplicationDescriptionProvider {
    ApplicationDescription get(String appId);
}

package com.arextest.storage.service.config.impl;

import com.arextest.storage.service.config.provider.ConfigProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

/**
 * @author niyan
 * @date 2024/6/6
 * @since 1.0.0
 */
@Slf4j
public class ApplicationPropertiesConfigProvider implements ConfigProvider {

    @Override
    public Map<String, String> loadConfigs(String configName) {
        // nothing to do
        return Collections.emptyMap();
    }

    @Override
    public void onChange(Map<String, String> configs) {
        // nothing to do
    }

}

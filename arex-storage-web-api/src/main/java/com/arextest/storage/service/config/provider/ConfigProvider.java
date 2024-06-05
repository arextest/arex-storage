package com.arextest.storage.service.config.provider;

import java.util.Map;

/**
 * @author niyan
 * @date 2024/6/6
 * @since 1.0.0
 */
public interface ConfigProvider {
    /**
     * load all configs
     */
    Map<String, String> loadConfigs(String configName);

    /**
     * replace all configs
     */
    void onChange(Map<String, String> configs);
}

package com.arextest.storage.service.config;

/**
 * @author niyan
 * @date 2024/6/14
 * @since 1.0.0
 */
public abstract class AbstractConfig {

    protected abstract String getConfigAsString(String key);

    public abstract String getConfigAsString(String key, String defaultValue);

    public abstract int getConfigAsInt(String key, int defaultValue);
}

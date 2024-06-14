package com.arextest.storage.service.config;

/**
 * @author niyan
 * @date 2024/6/14
 * @since 1.0.0
 */
public abstract class AbstractConfig {

    abstract protected String getConfigAsString(String key);

    abstract public String getConfigAsString(String key, String defaultValue);

    abstract public int getConfigAsInt(String key, int defaultValue);
}

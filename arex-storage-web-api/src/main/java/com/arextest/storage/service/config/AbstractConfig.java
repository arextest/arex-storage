package com.arextest.storage.service.config;

import org.apache.commons.lang3.StringUtils;

/**
 * @author niyan
 * @date 2024/6/14
 * @since 1.0.0
 */
public abstract class AbstractConfig {

  protected abstract String getConfigAsString(String key);

  public String getConfigAsString(String key, String defaultValue) {
    return StringUtils.defaultString(getConfigAsString(key), defaultValue);
  }

  public int getConfigAsInt(String key, int defaultValue) {
    String value = getConfigAsString(key);
    if (StringUtils.isBlank(value)) {
      return defaultValue;
    }

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public long getConfigAsLong(String key, long defaultValue) {
    String value = getConfigAsString(key);
    if (StringUtils.isBlank(value)) {
      return defaultValue;
    }

    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public boolean getConfigAsBoolean(String key, boolean defaultValue) {
    String value = getConfigAsString(key);
    if (StringUtils.isBlank(value)) {
      return defaultValue;
    }

    return Boolean.parseBoolean(value);
  }

}

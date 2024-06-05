package com.arextest.storage.service.config;

import com.arextest.storage.service.config.provider.ConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;

/**
 *
 * @author niyan
 * @date 2024/6/6
 * @since 1.0.0
 */
@Component
@Slf4j
public class ApplicationDefaultConfig {

    Map<String, String> configs;

    @Resource
    private Environment environment;
    @Resource
    private ConfigProvider configProvider;

    @PostConstruct
    public void init() {
        configs = configProvider.loadConfigs(null);
        configProvider.onChange(configs);
    }


    public String getConfigAsString(String key) {
        if (MapUtils.isNotEmpty(configs) && configs.containsKey(key)) {
            return configs.getOrDefault(key, StringUtils.EMPTY);
        }
        return environment.getProperty(key, String.class, StringUtils.EMPTY);
    }


    public int getConfigAsInt(String key, int defaultValue) {
        try {
            if (MapUtils.isNotEmpty(configs) && configs.containsKey(key) && configs.get(key) != null) {
                return Integer.parseInt(configs.get(key));
            }
            return environment.getProperty(key, Integer.class, defaultValue);
        } catch (NumberFormatException e) {
            LOGGER.error("Failed to parse config value for key: {}", key, e);
            return defaultValue;
        }
    }

}

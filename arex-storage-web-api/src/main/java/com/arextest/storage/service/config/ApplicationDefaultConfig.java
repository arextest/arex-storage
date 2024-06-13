package com.arextest.storage.service.config;

import com.arextest.storage.service.config.provider.ConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 *
 * @author niyan
 * @date 2024/6/6
 * @since 1.0.0
 */
@Component
@Slf4j
public class ApplicationDefaultConfig {

    @Resource(name = "applicationPropertiesConfigProvider")
    private ConfigProvider configProvider;


    public String getConfigAsString(String key) {
        return configProvider.getConfigAsString(key);
    }


    public int getConfigAsInt(String key, int defaultValue) {
        return configProvider.getConfigAsInt(key, defaultValue);
    }

}

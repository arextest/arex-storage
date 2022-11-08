package com.arextest.storage.beans;


import com.arextest.common.cache.CacheProvider;
import com.arextest.common.cache.DefaultRedisCacheProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jmo
 * @since 2021/10/18
 */
@Configuration
@EnableConfigurationProperties({StorageCacheProperties.class})
public class StorageCacheProviderAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(CacheProvider.class)
    public CacheProvider cacheProvider(StorageCacheProperties properties) {
        if (StringUtils.isEmpty(properties.getRedisHost())) {
            return new DefaultRedisCacheProvider();
        }
        return new DefaultRedisCacheProvider(properties.getRedisHost());
    }
}
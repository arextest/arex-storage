package com.arextest.storage.core.bean;


import com.arextest.common.cache.CacheProvider;
import com.arextest.common.cache.DefaultRedisCacheProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author jmo
 * @since 2021/10/18
 */
@Configuration
@EnableConfigurationProperties({StorageCacheProperties.class})
public class StorageCacheProviderAutoConfiguration {
    private final StorageCacheProperties properties;

    public StorageCacheProviderAutoConfiguration(StorageCacheProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean(CacheProvider.class)
    public CacheProvider cacheProvider() {
        CacheProvider provider = null;
        if (StringUtils.isNotEmpty(properties.getProvider())) {
            provider = this.lookup(CacheProvider.class, properties.getProvider());
        }
        if (provider == null) {
            provider = this.createDefaultCacheProvider();
        }
        return provider;
    }

    private CacheProvider createDefaultCacheProvider() {
        if (StringUtils.isEmpty(properties.getRedisHost())) {
            return new DefaultRedisCacheProvider();
        }
        return new DefaultRedisCacheProvider(properties.getRedisHost());
    }

    private <T> T lookup(@SuppressWarnings("SameParameterValue") Class<T> tClass, String name) {
        ServiceLoader<T> serviceLoader = ServiceLoader.load(tClass);
        Iterator<T> iterator = serviceLoader.iterator();
        while (iterator.hasNext()) {
            T instance = iterator.next();
            if (instance.getClass().getSimpleName().equals(name)) {
                return instance;
            }
        }
        return null;
    }
}
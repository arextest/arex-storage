package com.arextest.storage.web.api.service.bean;


import com.arextest.common.cache.CacheProvider;
import com.arextest.common.cache.DefaultRedisCacheProvider;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.internal.LoadingCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author jmo
 * @since 2021/10/18
 */
@Configuration
class StorageCacheConfiguration {
    @Value("${arex.storage.cache.redis.host:}")
    private String cacheHostUrl;
    @Value("${arex.storage.cache.provider:}")
    private String cacheProvider;
    @Value("${arex.storage.operation.cache.maxweight}")
    private long maximumWeight;

    @Bean
    CacheProvider cacheProvider() {
        CacheProvider provider = null;
        if (StringUtils.isNotEmpty(cacheProvider)) {
            provider = this.lookup(CacheProvider.class, cacheProvider);
        }
        if (provider == null) {
            provider = this.createDefaultCacheProvider();
        }
        return provider;
    }

    @Bean(name = "operationCache")
    public Cache<String, String> operationCache() {
        if (maximumWeight == 0) {
            maximumWeight = 2 * 1024 * 1024 * 1024;
        }
        Cache<String, String> cache = Caffeine
                .newBuilder()
                .initialCapacity(1000)
                .maximumWeight(maximumWeight)
                .weigher((String key, String value) -> key.length() + value.length())
                .build();
        return cache;
    }

    private CacheProvider createDefaultCacheProvider() {
        if (StringUtils.isEmpty(cacheHostUrl)) {
            return new DefaultRedisCacheProvider();
        }
        return new DefaultRedisCacheProvider(cacheHostUrl);
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

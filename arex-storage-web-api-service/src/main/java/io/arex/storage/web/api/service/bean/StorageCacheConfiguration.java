package io.arex.storage.web.api.service.bean;


import io.arex.common.cache.CacheProvider;
import io.arex.common.cache.DefaultRedisCacheProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
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

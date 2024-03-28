package com.arextest.storage.beans;

import com.arextest.common.cache.CacheProvider;
import com.arextest.common.cache.DefaultRedisCacheProvider;
import com.arextest.common.cache.SentinelRedisCacheProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wildeslam.
 * @create 2024/2/7 14:29
 */

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({StorageConfigurationProperties.class})
@Slf4j
public class RedisAutoConfiguration {

  private final StorageConfigurationProperties properties;

  public RedisAutoConfiguration(StorageConfigurationProperties configurationProperties) {
    properties = configurationProperties;
  }

  @Bean
  @ConditionalOnMissingBean(CacheProvider.class)
  public CacheProvider cacheProvider() {
    if (StringUtils.isNotEmpty(properties.getCache().getSentinelUrl())) {
      return new SentinelRedisCacheProvider(properties.getCache().getSentinelUrl());
    }
    if (StringUtils.isEmpty(properties.getCache().getUri())) {
      return new DefaultRedisCacheProvider();
    }
    return new DefaultRedisCacheProvider(properties.getCache().getUri());
  }
}

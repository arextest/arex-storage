package com.arextest.storage.beans;

import com.arextest.common.cache.CacheProvider;
import com.arextest.common.cache.DefaultRedisCacheProvider;
import com.arextest.common.cache.SentinelRedisCacheProvider;
import com.arextest.common.cache.redistemplate.RedisTemplateCacheProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * @author wildeslam.
 * @create 2024/2/7 14:29
 */

@Configuration(proxyBeanMethods = false)
@Slf4j
public class RedisAutoConfiguration {

  @Value("${arex.redis.uri:}")
  private String redisUri;

  @Value("${arex.redis.sentinelUrl:}")
  private String sentinelUrl;


  @Bean
  @ConditionalOnMissingBean(CacheProvider.class)
  @ConditionalOnExpression("!'${arex.redis.uri:}'.isEmpty() or !'${arex.redis.sentinelUrl:}'.isEmpty()")
  public CacheProvider cacheProvider() {
    if (StringUtils.isNotEmpty(sentinelUrl)) {
      return new SentinelRedisCacheProvider(sentinelUrl);
    }
    return new DefaultRedisCacheProvider(redisUri);
  }

  @Bean
  @ConditionalOnExpression("!'${arex.redis.uri:}'.isEmpty() or !'${arex.redis.sentinelUrl:}'.isEmpty()")
  public RedissonClient redissonClient(CacheProvider cacheProvider) {
    return cacheProvider.getRedissionClient();
  }

  @Bean
  @ConditionalOnExpression("'${arex.redis.uri:}'.isEmpty() and '${arex.redis.sentinelUrl:}'.isEmpty()")
  public RedisTemplate<byte[], byte[]> redisTemplate(
      RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<byte[], byte[]> template = new RedisTemplate<>();
    template.setConnectionFactory(redisConnectionFactory);
    template.setKeySerializer(RedisSerializer.byteArray());
    template.setValueSerializer(RedisSerializer.byteArray());
    return template;
  }

  @Bean
  @ConditionalOnMissingBean(CacheProvider.class)
  @ConditionalOnExpression("'${arex.redis.uri:}'.isEmpty() and '${arex.redis.sentinelUrl:}'.isEmpty()")
  public CacheProvider redisCacheProvider(RedisTemplate redisTemplate,
      RedissonClient redissonClient) {
    return new RedisTemplateCacheProvider(redisTemplate, redissonClient);
  }


}

package com.arextest.schedule.beans;


import com.arextest.common.cache.CacheProvider;
import com.arextest.common.cache.DefaultRedisCacheProvider;
import com.arextest.common.cache.SentinelRedisCacheProvider;
import com.arextest.common.cache.redistemplate.RedisTemplateCacheProvider;
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
 * @author jmo
 * @since 2021/10/18
 */
@Configuration
@SuppressWarnings("java:S6857")
class CacheProviderConfiguration {

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
  @ConditionalOnExpression("'${arex.redis.uri:}'.isEmpty() and '${arex.redis.sentinelUrl:}'.isEmpty()")
  @ConditionalOnMissingBean(CacheProvider.class)
  public CacheProvider redisCacheProvider(RedisTemplate redisTemplate,
      RedissonClient redissonClient) {
    return new RedisTemplateCacheProvider(redisTemplate, redissonClient);
  }
}
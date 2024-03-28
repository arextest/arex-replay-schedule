package com.arextest.schedule.beans;


import com.arextest.common.cache.CacheProvider;
import com.arextest.common.cache.DefaultRedisCacheProvider;
import com.arextest.common.cache.SentinelRedisCacheProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jmo
 * @since 2021/10/18
 */
@Configuration
@ConditionalOnMissingBean(CacheProvider.class)
class CacheProviderConfiguration {

  @Value("${arex.redis.uri}")
  private String scheduleCacheRedisHost;

  @Value("${arex.redis.sentinelUrl:}")
  private String sentinelUrl;

  @Bean
  CacheProvider cacheProvider() {
    if (StringUtils.isNotEmpty(sentinelUrl)) {
      return new SentinelRedisCacheProvider(sentinelUrl);
    }
    return new DefaultRedisCacheProvider(scheduleCacheRedisHost);
  }
}
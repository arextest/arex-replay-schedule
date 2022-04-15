package io.arex.replay.schedule.beans;


import io.arex.common.cache.CacheProvider;
import io.arex.common.cache.DefaultRedisCacheProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jmo
 * @since 2021/10/18
 */
@Configuration
class RedisCacheProviderConfiguration {
    @Value("${arex.schedule.cache.redis.host}")
    private String scheduleCacheRedisHost;

    @Bean
    CacheProvider redisCacheProvider() {
        return new DefaultRedisCacheProvider(scheduleCacheRedisHost);
    }
}

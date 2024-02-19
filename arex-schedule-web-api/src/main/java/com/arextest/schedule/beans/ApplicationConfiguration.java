package com.arextest.schedule.beans;

import com.arextest.schedule.service.ConfigProvider;
import com.arextest.schedule.service.DefaultConfigProviderImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * created by xinyuan_wang on 2024/2/19
 * <p>
 * Expose the entrance for users to inherit.
 */
@Slf4j
@Configuration
public class ApplicationConfiguration {

  @Bean
  @ConditionalOnMissingBean(ConfigProvider.class)
  public ConfigProvider configProvider() {
    return new DefaultConfigProviderImpl();
  }
}
package com.arextest.schedule.beans;

import com.arextest.schedule.service.MetricListener;
import com.arextest.schedule.service.MetricService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * record log created by xinyuan_wang on 2023/2/20
 */
@Slf4j
@Configuration
@ConditionalOnMissingBean(MetricService.class)
public class MetricConfiguration {

  @Bean
  public MetricService metricService(List<MetricListener> metricListeners) {
    LOGGER.warn("Could not found any MetricListener, please check your configuration");
    return new MetricService(metricListeners);
  }
}

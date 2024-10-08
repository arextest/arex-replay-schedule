package com.arextest.schedule.beans;

import com.arextest.extension.desensitization.DataDesensitization;
import com.arextest.schedule.serialization.DesensitizationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;


@Configuration
@Slf4j
public class DataDesensitizationConfiguration {

  @Bean
  @ConditionalOnMissingBean(DesensitizationProvider.class)
  DesensitizationProvider desensitizationProvider(MongoDatabaseFactory factory) {
    return new DesensitizationProvider(factory.getMongoDatabase());
  }

  @Bean
  DataDesensitization dataDesensitization(DesensitizationProvider desensitizationProvider) {
    return desensitizationProvider.get();
  }


}

package com.arextest.schedule.beans;

import com.arextest.common.config.ConfigProvider;
import com.arextest.common.config.DefaultApplicationConfig;
import com.arextest.common.config.DefaultConfigProvider;
import com.arextest.common.jwt.JWTService;
import com.arextest.common.jwt.JWTServiceImpl;
import com.arextest.schedule.aspect.AppAuthAspectExecutor;
import com.arextest.schedule.comparer.ReplayResultComparer;
import com.arextest.schedule.comparer.impl.PrepareCompareSourceRemoteLoader;
import com.arextest.schedule.dao.mongodb.ApplicationRepository;
import com.arextest.schedule.dao.mongodb.ReplayCompareResultRepositoryImpl;
import com.arextest.schedule.dao.mongodb.ReplayNoiseRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanActionRepository;
import com.arextest.schedule.sender.ReplaySenderFactory;
import com.arextest.schedule.service.noise.ReplayNoiseIdentify;
import com.arextest.schedule.service.noise.ReplayNoiseIdentifyService;
import java.util.concurrent.ExecutorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class ServiceAutoConfiguration {

  private static final long ACCESS_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;
  private static final long REFRESH_EXPIRE_TIME = 30 * 24 * 60 * 60 * 1000L;

  @Value("${arex.jwt.secret:arex}")
  private String tokenSecret;

  @Bean
  @ConditionalOnMissingBean(ReplayNoiseIdentify.class)
  public ReplayNoiseIdentify replayNoiseIdentify(
      ReplayResultComparer replayResultComparer,
      ReplayCompareResultRepositoryImpl replayCompareResultRepository,
      ReplayNoiseRepository replayNoiseRepository,
      ReplayPlanActionRepository replayPlanActionRepository,
      ReplaySenderFactory replaySenderFactory,
      PrepareCompareSourceRemoteLoader sourceRemoteLoader, ExecutorService sendExecutorService,
      ExecutorService analysisNoiseExecutorService) {
    return new ReplayNoiseIdentifyService(
        replayResultComparer,
        replayCompareResultRepository,
        replayNoiseRepository, replayPlanActionRepository, replaySenderFactory, sourceRemoteLoader,
        sendExecutorService, analysisNoiseExecutorService);
  }

  @Bean
  @ConditionalOnMissingBean(AppAuthAspectExecutor.class)
  public AppAuthAspectExecutor appAuthAspectExecutor(ApplicationRepository applicationRepository,
      MongoTemplate mongoTemplate, JWTService jwtService) {
    return new AppAuthAspectExecutor(applicationRepository, mongoTemplate, jwtService);
  }

  @Bean
  @ConditionalOnMissingBean(JWTService.class)
  public JWTService jwtService() {
    return new JWTServiceImpl(ACCESS_EXPIRE_TIME, REFRESH_EXPIRE_TIME, tokenSecret);
  }

  @Bean
  @ConditionalOnMissingBean(ConfigProvider.class)
  public ConfigProvider defaultConfigProvider(Environment environment) {
    return new DefaultConfigProvider(environment);
  }

  @Bean
  public DefaultApplicationConfig defaultApplicationConfig(ConfigProvider configProvider) {
    return new DefaultApplicationConfig(configProvider);
  }


}

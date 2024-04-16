package com.arextest.schedule.beans;

import com.arextest.schedule.comparer.ReplayResultComparer;
import com.arextest.schedule.comparer.impl.PrepareCompareSourceRemoteLoader;
import com.arextest.schedule.dao.mongodb.ReplayCompareResultRepositoryImpl;
import com.arextest.schedule.dao.mongodb.ReplayNoiseRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanActionRepository;
import com.arextest.schedule.sender.ReplaySenderFactory;
import com.arextest.schedule.service.noise.ReplayNoiseIdentify;
import com.arextest.schedule.service.noise.ReplayNoiseIdentifyService;
import java.util.concurrent.ExecutorService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceAutoConfiguration {

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
}

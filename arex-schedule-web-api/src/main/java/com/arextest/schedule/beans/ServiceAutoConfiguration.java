package com.arextest.schedule.beans;

import java.util.concurrent.ExecutorService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.arextest.schedule.comparer.CompareConfigService;
import com.arextest.schedule.comparer.ComparisonWriter;
import com.arextest.schedule.comparer.CustomComparisonConfigurationHandler;
import com.arextest.schedule.comparer.impl.PrepareCompareSourceRemoteLoader;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.dao.mongodb.ReplayCompareResultRepositoryImpl;
import com.arextest.schedule.dao.mongodb.ReplayNoiseRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanActionRepository;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.sender.ReplaySenderFactory;
import com.arextest.schedule.service.MetricService;
import com.arextest.schedule.service.noise.ReplayNoiseIdentify;
import com.arextest.schedule.service.noise.ReplayNoiseIdentifyService;

@Configuration
public class ServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ReplayNoiseIdentify.class)
    public ReplayNoiseIdentify replayNoiseIdentify(CompareConfigService compareConfigService,
        ProgressTracer progressTracer, ComparisonWriter comparisonOutputWriter,
        ReplayActionCaseItemRepository caseItemRepository, MetricService metricService,
        CustomComparisonConfigurationHandler customComparisonConfigurationHandler,
        ReplayCompareResultRepositoryImpl replayCompareResultRepository, ReplayNoiseRepository replayNoiseRepository,
        ReplayPlanActionRepository replayPlanActionRepository, ReplaySenderFactory replaySenderFactory,
        PrepareCompareSourceRemoteLoader sourceRemoteLoader, ExecutorService sendExecutorService,
        ExecutorService analysisNoiseExecutorService) {
        return new ReplayNoiseIdentifyService(compareConfigService, progressTracer, comparisonOutputWriter,
            caseItemRepository, metricService, customComparisonConfigurationHandler, replayCompareResultRepository,
            replayNoiseRepository, replayPlanActionRepository, replaySenderFactory, sourceRemoteLoader,
            sendExecutorService, analysisNoiseExecutorService);
    }
}

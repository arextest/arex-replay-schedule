package com.arextest.schedule.beans;

import com.arextest.schedule.comparer.CompareConfigExtensionService;
import com.arextest.schedule.comparer.CompareConfigService;
import com.arextest.schedule.comparer.ComparisonWriter;
import com.arextest.schedule.comparer.ReplayResultComparer;
import com.arextest.schedule.comparer.impl.DefaultCompareConfigExtensionService;
import com.arextest.schedule.comparer.impl.DefaultReplayResultComparer;
import com.arextest.schedule.comparer.impl.PrepareCompareSourceRemoteLoader;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.service.MetricService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Configuration
public class ReplayComparerConfiguration {
    @Bean
    @ConditionalOnMissingBean(ReplayResultComparer.class)
    public ReplayResultComparer defaultResultComparer(
            CompareConfigService compareConfigService,
            PrepareCompareSourceRemoteLoader sourceRemoteLoader,
            ProgressTracer progressTracer,
            ComparisonWriter comparisonOutputWriter,
            ReplayActionCaseItemRepository caseItemRepository,
            MetricService metricService
    ) {
        return new DefaultReplayResultComparer(compareConfigService,
                sourceRemoteLoader,
                progressTracer,
                comparisonOutputWriter,
                caseItemRepository,
                metricService);
    }

    @Bean
    @ConditionalOnMissingBean(CompareConfigExtensionService.class)
    public CompareConfigExtensionService compareConfigPreService() {
        return new DefaultCompareConfigExtensionService();
    }
}
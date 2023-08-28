package com.arextest.schedule.beans;

import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.planexecution.impl.DefaultExecutionContextProvider;
import com.arextest.schedule.planexecution.PlanExecutionContextProvider;
import com.arextest.schedule.sender.ReplaySenderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Configuration
public class ExecutionContextConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public PlanExecutionContextProvider<?> defaultExecutionContextBuilder(
            ReplayActionCaseItemRepository replayActionCaseItemRepository,
            ReplaySenderFactory replaySenderFactory
    ) {
        return new DefaultExecutionContextProvider(
                replayActionCaseItemRepository,
                replaySenderFactory
        );
    }
}
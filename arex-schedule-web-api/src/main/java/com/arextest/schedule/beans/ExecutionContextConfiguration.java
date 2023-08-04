package com.arextest.schedule.beans;

import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.planexecution.impl.DefaultExecutionContextProvider;
import com.arextest.schedule.planexecution.PlanExecutionContextProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Configuration
public class ExecutionContextConfiguration {
    @Bean
    public PlanExecutionContextProvider<?> defaultExecutionContextBuilder(
            ReplayActionCaseItemRepository replayActionCaseItemRepository) {
        return new DefaultExecutionContextProvider(replayActionCaseItemRepository);
    }
}
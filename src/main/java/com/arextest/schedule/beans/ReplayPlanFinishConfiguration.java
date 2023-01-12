package com.arextest.schedule.beans;

import com.arextest.schedule.service.PlanFinishListener;
import com.arextest.schedule.service.PlanFinishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * created by xinyuan_wang on 2023/1/12
 */
@Slf4j
@Configuration
@ConditionalOnMissingBean(PlanFinishService.class)
public class ReplayPlanFinishConfiguration {
    @Bean
    public PlanFinishService planFinishService(
            List<PlanFinishListener> planFinishListeners
    ) {
        return new PlanFinishService(planFinishListeners);
    }
}
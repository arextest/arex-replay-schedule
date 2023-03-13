package com.arextest.schedule.beans;

import com.arextest.schedule.service.DeployedEnvironmentService;
import com.arextest.schedule.service.ScheduleConfigurationListener;
import com.arextest.schedule.service.ScheduleConfigurationService;
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
@ConditionalOnMissingBean(DeployedEnvironmentService.class)
public class ScheduleConfiguration {
    @Bean
    public ScheduleConfigurationService scheduleConfigurationService(
            List<ScheduleConfigurationListener> scheduleConfigurationListeners
    ) {
        return new ScheduleConfigurationService(scheduleConfigurationListeners);
    }
}
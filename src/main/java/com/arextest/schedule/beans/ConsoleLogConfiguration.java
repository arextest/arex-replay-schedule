package com.arextest.schedule.beans;

import com.arextest.schedule.service.ConsoleLogListener;
import com.arextest.schedule.service.ConsoleLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


/**
 * record log
 * created by xinyuan_wang on 2023/2/20
 */
@Slf4j
@Configuration
@ConditionalOnMissingBean(ConsoleLogService.class)
public class ConsoleLogConfiguration {
    @Bean
    public ConsoleLogService consoleLogService(
            List<ConsoleLogListener> consoleLogListeners
    ) {
        return new ConsoleLogService(consoleLogListeners);
    }
}

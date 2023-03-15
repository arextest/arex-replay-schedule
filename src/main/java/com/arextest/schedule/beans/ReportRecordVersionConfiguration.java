package com.arextest.schedule.beans;

import com.arextest.schedule.service.DefaultRecordVersionUrlProviderImpl;
import com.arextest.schedule.service.RecordVersionUrlProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: sldu
 * @date: 2023/3/15 15:18
 **/
@Slf4j
@Configuration
@ConditionalOnMissingBean(RecordVersionUrlProvider.class)
public class ReportRecordVersionConfiguration {
    @Bean
    public RecordVersionUrlProvider defaultRecordVersionProvider(){
        return new DefaultRecordVersionUrlProviderImpl();
    }
}

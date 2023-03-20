package com.arextest.schedule.beans;

import com.arextest.schedule.service.RecordVersionUrlProvider;
import com.arextest.schedule.service.ReportRecordVersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author: sldu
 * @date: 2023/3/20 11:40
 **/
@Slf4j
@Configuration
@ConditionalOnMissingBean(ReportRecordVersionService.class)
public class ReportRecordVersionConfiguration {
    @Bean
    public ReportRecordVersionService defaultRecordVersionProvider(List<RecordVersionUrlProvider> recordVersionUrlProviderList){
        return new ReportRecordVersionService(recordVersionUrlProviderList);
    }
}

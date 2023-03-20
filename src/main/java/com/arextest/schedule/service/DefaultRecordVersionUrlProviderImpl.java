package com.arextest.schedule.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author: sldu
 * @date: 2023/3/20 11:39
 **/
@Component
public class DefaultRecordVersionUrlProviderImpl implements RecordVersionUrlProvider{
    @Value("${arex.report.config.applicationInstances.url}")
    private String queryRecordVersionUrl;

    @Override
    public String getRecordVersionUrl(int env){
        return queryRecordVersionUrl;
    }
}

package com.arextest.schedule.beans;

import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.progress.impl.UpdateResultProgressEventImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by Qzmo on 2023/6/20
 */
@Configuration
public class ProgressEventConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public ProgressEvent progressEvent() {
        return new UpdateResultProgressEventImpl();
    }
}

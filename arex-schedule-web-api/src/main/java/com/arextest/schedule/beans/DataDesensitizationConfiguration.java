package com.arextest.schedule.beans;

import com.arextest.common.model.classloader.RemoteJarClassLoader;
import com.arextest.common.utils.RemoteJarLoaderUtils;
import com.arextest.extension.desensitization.DataDesensitization;
import com.arextest.extension.desensitization.DefaultDataDesensitization;
import com.arextest.schedule.service.ConfigurationService;
import com.arextest.web.model.contract.contracts.datadesensitization.DesensitizationJar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;


@Configuration
@Slf4j
public class DataDesensitizationConfiguration {
    @Resource
    ConfigurationService configurationService;

    @Bean
    DataDesensitization desensitizationService() {
        List<DesensitizationJar> uploaded = configurationService.desensitization();
        if (CollectionUtils.isEmpty(uploaded)) {
            return new DefaultDataDesensitization();
        } else {
            try {
                DesensitizationJar selected = uploaded.get(uploaded.size() - 1);
                RemoteJarClassLoader classLoader = RemoteJarLoaderUtils.loadJar(selected.getJarUrl());
                return RemoteJarLoaderUtils.loadService(DataDesensitization.class, classLoader).get(0);
            } catch (Throwable t) {
                LOGGER.error("Load remote desensitization jar failed, application startup blocked", t);
                throw new RuntimeException("Load remote desensitization jar failed");
            }
        }
    }
}

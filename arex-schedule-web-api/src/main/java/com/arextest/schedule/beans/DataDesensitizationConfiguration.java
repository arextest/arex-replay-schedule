package com.arextest.schedule.beans;

import com.arextest.common.utils.PluginClassLoaderUtils;
import com.arextest.desensitization.extension.DataDesensitization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class DataDesensitizationConfiguration {

    @Bean
    DataDesensitization desensitizationService() {
        // todo load remote jar
        PluginClassLoaderUtils.loadJar("http://maven.release.ctripcorp.com/nexus/content/repositories/flightsnapshot/com/arextest/arex-desensitization-core/0.0.1-SNAPSHOT/arex-desensitization-core-0.0.1-20230824.075204-3.jar");

        // intended to fail on null bean
        return PluginClassLoaderUtils.loadService(DataDesensitization.class).get(0);
    }
}

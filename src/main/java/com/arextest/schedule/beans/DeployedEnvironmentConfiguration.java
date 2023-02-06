package com.arextest.schedule.beans;

import com.arextest.schedule.model.deploy.DeploymentEnvironmentProvider;
import com.arextest.schedule.service.DeployedEnvironmentService;
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
public class DeployedEnvironmentConfiguration {
    @Bean
    public DeployedEnvironmentService planFinishService(
            List<DeploymentEnvironmentProvider> deploymentEnvironmentProviders
    ) {
        return new DeployedEnvironmentService(deploymentEnvironmentProviders);
    }
}
package com.arextest.schedule.service;


import com.arextest.schedule.model.AppServiceDescriptor;
import com.arextest.schedule.model.deploy.DeploymentEnvironmentProvider;
import com.arextest.schedule.model.deploy.DeploymentVersion;
import com.arextest.schedule.model.deploy.ServiceInstance;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * created by xinyuan_wang on 2023/4/24
 */
@Service
@Slf4j
public class DefaultDeployedEnvironmentService {
    private final List<DeploymentEnvironmentProvider> environmentProviderList;

    public DefaultDeployedEnvironmentService(List<DeploymentEnvironmentProvider> environmentProviderList) {
        this.environmentProviderList = environmentProviderList;
    }

    public DeploymentVersion getVersionEvent(String appId, String env) {
        if (CollectionUtils.isEmpty(this.environmentProviderList)) {
            return null;
        }
        for (DeploymentEnvironmentProvider provider : this.environmentProviderList) {
            return provider.getVersion(appId, env);
        }
        return null;
    }

    public List<ServiceInstance> getActiveInstanceListEvent(AppServiceDescriptor serviceDescriptor, String env) {
        if (CollectionUtils.isEmpty(this.environmentProviderList)) {
            return Collections.emptyList();
        }
        for (DeploymentEnvironmentProvider provider : this.environmentProviderList) {
            return provider.getActiveInstanceList(serviceDescriptor, env);
        }
        return Collections.emptyList();
    }

    public ServiceInstance getActiveInstanceEvent(AppServiceDescriptor serviceDescriptor, String host) {
        if (CollectionUtils.isEmpty(this.environmentProviderList)) {
            return null;
        }
        for (DeploymentEnvironmentProvider provider : this.environmentProviderList) {
            return provider.getActiveInstance(serviceDescriptor, host);
        }
        return null;
    }

}
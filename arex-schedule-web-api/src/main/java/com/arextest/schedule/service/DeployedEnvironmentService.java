package com.arextest.schedule.service;

import com.arextest.schedule.model.AppServiceDescriptor;
import com.arextest.schedule.model.deploy.DeploymentEnvironmentProvider;
import com.arextest.schedule.model.deploy.DeploymentVersion;
import com.arextest.schedule.model.deploy.ServiceInstance;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author jmo
 * @since 2021/9/13
 */
@Service
@Slf4j
public class DeployedEnvironmentService {
    private final List<DeploymentEnvironmentProvider> environmentProviderList;

    public DeployedEnvironmentService(List<DeploymentEnvironmentProvider> environmentProviderList) {
        this.environmentProviderList = environmentProviderList;
    }

    public DeploymentVersion getVersion(String appId, String env) {
        if (CollectionUtils.isEmpty(this.environmentProviderList)) {
            return null;
        }
        DeploymentVersion deploymentVersion = null;
        for (DeploymentEnvironmentProvider provider : this.environmentProviderList) {
            deploymentVersion = provider.getVersion(appId, env);
            break;
        }
        return deploymentVersion;
    }

    public List<ServiceInstance> getActiveInstanceList(AppServiceDescriptor serviceDescriptor, String env) {
        if (CollectionUtils.isEmpty(this.environmentProviderList)) {
            return Collections.emptyList();
        }
        List<ServiceInstance> serviceInstanceList = new ArrayList<>();
        for (DeploymentEnvironmentProvider provider : this.environmentProviderList) {
            serviceInstanceList = provider.getActiveInstanceList(serviceDescriptor, env);
            break;
        }
        return serviceInstanceList;
    }

    public ServiceInstance getActiveInstance(AppServiceDescriptor serviceDescriptor, String host) {
        if (CollectionUtils.isEmpty(this.environmentProviderList)) {
            return null;
        }
        ServiceInstance serviceInstance = null;
        for (DeploymentEnvironmentProvider provider : this.environmentProviderList) {
            serviceInstance =  provider.getActiveInstance(serviceDescriptor, host);
            break;
        }
        return serviceInstance;
    }

}
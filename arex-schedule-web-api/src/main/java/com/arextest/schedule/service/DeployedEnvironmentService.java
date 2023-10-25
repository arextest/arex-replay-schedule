package com.arextest.schedule.service;

import com.arextest.schedule.model.AppServiceDescriptor;
import com.arextest.schedule.model.deploy.DeploymentEnvironmentProvider;
import com.arextest.schedule.model.deploy.DeploymentVersion;
import com.arextest.schedule.model.deploy.ServiceInstance;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

/**
 * @author jmo
 * @since 2021/9/13
 */
@Service
@Slf4j
public class DeployedEnvironmentService {

  private static final int FIRST_INDEX = 0;
  private final List<DeploymentEnvironmentProvider> environmentProviderList;

  public DeployedEnvironmentService(List<DeploymentEnvironmentProvider> environmentProviderList) {
    this.environmentProviderList = environmentProviderList;
  }

  public DeploymentVersion getVersion(String appId, String env) {
    if (CollectionUtils.isEmpty(this.environmentProviderList)) {
      return null;
    }
    return this.environmentProviderList.get(FIRST_INDEX).getVersion(appId, env);
  }

  public List<ServiceInstance> getActiveInstanceList(AppServiceDescriptor serviceDescriptor,
      String env) {
    if (CollectionUtils.isEmpty(this.environmentProviderList)) {
      return Collections.emptyList();
    }
    return this.environmentProviderList.get(FIRST_INDEX)
        .getActiveInstanceList(serviceDescriptor, env);
  }

  public ServiceInstance getActiveInstance(AppServiceDescriptor serviceDescriptor, String host) {
    if (CollectionUtils.isEmpty(this.environmentProviderList)) {
      return null;
    }
    return this.environmentProviderList.get(FIRST_INDEX).getActiveInstance(serviceDescriptor, host);
  }

}
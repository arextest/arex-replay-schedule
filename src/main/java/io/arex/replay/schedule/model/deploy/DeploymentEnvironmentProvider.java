package io.arex.replay.schedule.model.deploy;

import io.arex.replay.schedule.model.AppServiceDescriptor;

import java.util.List;

/**
 * @author jmo
 * @since 2022/2/19
 */
public interface DeploymentEnvironmentProvider {
    DeploymentVersion getVersion(String appId, String env);

    List<ServiceInstance> getActiveInstanceList(AppServiceDescriptor serviceDescriptor, String env);

    ServiceInstance getActiveInstance(AppServiceDescriptor serviceDescriptor, String ip);
}

package io.arex.replay.schedule.plan;

import io.arex.replay.schedule.model.AppServiceDescriptor;
import io.arex.replay.schedule.model.AppServiceOperationDescriptor;
import io.arex.replay.schedule.model.ReplayActionItem;
import io.arex.replay.schedule.model.deploy.DeploymentVersion;
import io.arex.replay.schedule.model.deploy.ServiceInstance;
import io.arex.replay.schedule.model.deploy.ServiceInstanceOperation;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.function.Function;

/**
 * @author jmo
 * @since 2021/9/22
 */
@Data
public final class PlanContext {
    private List<AppServiceDescriptor> appServiceDescriptorList;
    private DeploymentVersion targetVersion;
    private DeploymentVersion sourceVersion;
    private String appId;


    public AppServiceOperationDescriptor findAppServiceOperationDescriptor(long operationId) {
        List<AppServiceOperationDescriptor> operationDescriptorList;
        for (AppServiceDescriptor appServiceDescriptor : appServiceDescriptorList) {
            operationDescriptorList = appServiceDescriptor.getOperationList();
            if (CollectionUtils.isEmpty(operationDescriptorList)) {
                continue;
            }
            for (AppServiceOperationDescriptor operationDescriptor : operationDescriptorList) {
                if (operationId == operationDescriptor.getId()) {
                    return operationDescriptor;
                }
            }
        }
        return null;
    }

    public ServiceInstance targetActiveInstance() {
        return firstActiveInstance(AppServiceDescriptor::getTargetActiveInstanceList);
    }

    private ServiceInstance firstActiveInstance(Function<AppServiceDescriptor, List<ServiceInstance>> source) {
        for (AppServiceDescriptor appServiceDescriptor : appServiceDescriptorList) {
            List<ServiceInstance> instanceList = source.apply(appServiceDescriptor);
            if (CollectionUtils.isNotEmpty(instanceList)) {
                return instanceList.get(0);
            }
        }
        return null;
    }

    private ServiceInstance firstActiveInstance(List<ServiceInstance> instanceList) {
        if (CollectionUtils.isNotEmpty(instanceList)) {
            return instanceList.get(0);
        }
        return null;
    }

    public ServiceInstance sourceActiveInstance() {
        return firstActiveInstance(AppServiceDescriptor::getSourceActiveInstanceList);
    }

    public ReplayActionItem toReplayAction(AppServiceOperationDescriptor operationDescriptor) {
        ReplayActionItem replayActionItem = new ReplayActionItem();
        fillReplayAction(replayActionItem, operationDescriptor);
        return replayActionItem;
    }

    public void fillReplayAction(ReplayActionItem replayActionItem, AppServiceOperationDescriptor operationDescriptor) {
        AppServiceDescriptor serviceDescriptor = operationDescriptor.getParent();
        replayActionItem.setAppId(serviceDescriptor.getAppId());
        final String operationName = operationDescriptor.getOperationName();
        ServiceInstance activeInstance = firstActiveInstance(serviceDescriptor.getTargetActiveInstanceList());
        replayActionItem.setTargetInstance(activeInstance);
        replayActionItem.setSourceInstance(firstActiveInstance(serviceDescriptor.getSourceActiveInstanceList()));
        replayActionItem.setOperationName(operationName);
        replayActionItem.setActionType(operationDescriptor.getOperationType());
        replayActionItem.setServiceKey(serviceDescriptor.getServiceKey());
        replayActionItem.setServiceName(serviceDescriptor.getServiceName());
        replayActionItem.setOperationId(operationDescriptor.getId());
        replayActionItem.setMappedInstanceOperation(this.findActiveOperation(operationName, activeInstance));
    }

    private ServiceInstanceOperation findActiveOperation(String operation, ServiceInstance activeInstance) {
        if (activeInstance != null) {
            List<ServiceInstanceOperation> operationList = activeInstance.getOperationList();
            for (ServiceInstanceOperation serviceInstanceOperation : operationList) {
                if (StringUtils.equals(operation, serviceInstanceOperation.getName())) {
                    return serviceInstanceOperation;
                }
            }
        }
        return null;
    }
}

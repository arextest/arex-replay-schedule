package com.arextest.schedule.plan;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.schedule.model.AppServiceDescriptor;
import com.arextest.schedule.model.AppServiceOperationDescriptor;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.deploy.DeploymentVersion;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.model.deploy.ServiceInstanceOperation;
import com.google.common.collect.Lists;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static com.arextest.schedule.common.CommonConstant.SOAPROVIDER;

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

    public AppServiceOperationDescriptor findAppServiceOperationDescriptor(String operationId) {
        List<AppServiceOperationDescriptor> operationDescriptorList;
        for (AppServiceDescriptor appServiceDescriptor : appServiceDescriptorList) {
            operationDescriptorList = appServiceDescriptor.getOperationList();
            if (CollectionUtils.isEmpty(operationDescriptorList)) {
                continue;
            }
            for (AppServiceOperationDescriptor operationDescriptor : operationDescriptorList) {
                if (Objects.equals(operationId, operationDescriptor.getId())) {
                    return operationDescriptor;
                }
            }
        }
        return null;
    }

    public List<ServiceInstance> targetActiveInstance() {
        return getActiveInstance(AppServiceDescriptor::getTargetActiveInstanceList);
    }

    private List<ServiceInstance> getActiveInstance(Function<AppServiceDescriptor, List<ServiceInstance>> source) {
        List<ServiceInstance> instanceList = Lists.newArrayList();
        for (AppServiceDescriptor descriptor : appServiceDescriptorList) {
            if (CollectionUtils.isEmpty(descriptor.getTargetActiveInstanceList())) {
                break;
            }
            List<ServiceInstance> targetActiveInstanceList = descriptor.getTargetActiveInstanceList();
            if (StringUtils.isEmpty(targetActiveInstanceList.get(0).getIp())) {
                break;
            }
            instanceList.addAll(source.apply(descriptor));
        }
        return instanceList;
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

    public List<ServiceInstance> sourceActiveInstance() {
        return getSourceActiveInstance(AppServiceDescriptor::getSourceActiveInstanceList);
    }

    private List<ServiceInstance> getSourceActiveInstance(Function<AppServiceDescriptor, List<ServiceInstance>> source) {
        List<ServiceInstance> instanceList = Lists.newArrayList();
        for (AppServiceDescriptor descriptor : appServiceDescriptorList) {
            if (CollectionUtils.isEmpty(descriptor.getSourceActiveInstanceList())) {
                break;
            }
            List<ServiceInstance> sourceActiveInstanceList = descriptor.getSourceActiveInstanceList();
            if (StringUtils.isEmpty(sourceActiveInstanceList.get(0).getIp())) {
                break;
            }
            instanceList.addAll(source.apply(descriptor));
        }
        return instanceList;
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
        replayActionItem.setTargetInstance(serviceDescriptor.getTargetActiveInstanceList());
        replayActionItem.setSourceInstance(serviceDescriptor.getSourceActiveInstanceList());
        replayActionItem.setOperationName(operationName);
        replayActionItem.setSourceInstance(serviceDescriptor.getSourceActiveInstanceList());
        replayActionItem.setActionType(operationType);
        replayActionItem.setServiceKey(serviceDescriptor.getServiceKey());
        replayActionItem.setServiceName(serviceDescriptor.getServiceName());
        replayActionItem.setOperationId(operationDescriptor.getId());
        replayActionItem.setMappedInstanceOperation(this.findActiveOperation(operationName, serviceDescriptor.getTargetActiveInstanceList().get(0)));
    }
    
    private ServiceInstanceOperation findActiveOperation(String operation, List<ServiceInstance> activeInstances) {
        if (CollectionUtils.isEmpty(activeInstances)) {
            return null;
        }
        List<ServiceInstanceOperation> operationList = activeInstances.get(0).getOperationList();
        if (CollectionUtils.isEmpty(operationList)) {
            return null;
        }
        for (ServiceInstanceOperation serviceInstanceOperation : operationList) {
            if (StringUtils.equals(operation, serviceInstanceOperation.getName())) {
                return serviceInstanceOperation;
            }
        }
        return null;
    }

    private String getShortOperationName(String operationType, String operationName) {
        if (operationType.equals(SOAPROVIDER) || operationType.equals(MockCategoryType.DUBBO_PROVIDER.getName())) {
            String[] split = operationName.split("\\.");
            operationName = split[split.length - 1];
        }
        return operationName;
    }

    private List<ServiceInstance> findActiveInstances(AppServiceDescriptor serviceDescriptor, String operationName, String operationType) {
        List<ServiceInstance> targetActiveInstanceList = serviceDescriptor.getTargetActiveInstanceList();
        if (operationType.equals(MockCategoryType.SERVLET.getName())) {
            return targetActiveInstanceList;
        }
        List<ServiceInstance> newTargetInstanceList = Lists.newArrayList();
        for (ServiceInstance instance : targetActiveInstanceList) {
            List<ServiceInstanceOperation> operationList = instance.getOperationList();
            if (CollectionUtils.isEmpty(operationList)) {
                continue;
            }

            Optional<ServiceInstanceOperation> any = operationList.stream().filter(operation -> operation.getName().equals(operationName)).findAny();
            if (any.isPresent()) {
                newTargetInstanceList.add(instance);
            }
        }
        return newTargetInstanceList;
    }
}
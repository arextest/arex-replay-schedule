package com.arextest.schedule.plan;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.schedule.model.AppServiceDescriptor;
import com.arextest.schedule.model.AppServiceOperationDescriptor;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.deploy.DeploymentVersion;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.model.deploy.ServiceInstanceOperation;
import com.arextest.schedule.service.DeployedEnvironmentService;
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

    @Resource
    private DeployedEnvironmentService service;


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
        Optional<AppServiceDescriptor> any = appServiceDescriptorList.stream().filter(
                data -> data.getServiceName() != MockCategoryType.Q_MESSAGE_CONSUMER.getName() &&
                        data.getServiceName() != MockCategoryType.SERVLET.getName()).findAny();
        if (any.isPresent()) {
            instanceList.addAll(source.apply(any.get()));
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
        Optional<AppServiceDescriptor> any = appServiceDescriptorList.stream().filter(
                data -> CollectionUtils.isNotEmpty(data.getSourceActiveInstanceList())).findAny();
        if (any.isPresent()) {
            return getActiveInstance(AppServiceDescriptor::getSourceActiveInstanceList);
        } else {
            return Collections.emptyList();
        }
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
        String operationType = operationDescriptor.getOperationType();
        String shortOperationName = getShortOperationName(operationType, operationName);
        List<ServiceInstance> activeInstances = findActiveInstances(serviceDescriptor, shortOperationName);
        replayActionItem.setTargetInstance(activeInstances);
        replayActionItem.setOperationName(operationName);
        replayActionItem.setSourceInstance(serviceDescriptor.getSourceActiveInstanceList());
        replayActionItem.setActionType(operationType);
        replayActionItem.setServiceKey(serviceDescriptor.getServiceKey());
        replayActionItem.setServiceName(serviceDescriptor.getServiceName());
        replayActionItem.setOperationId(operationDescriptor.getId());
        replayActionItem.setMappedInstanceOperation(this.findActiveOperation(shortOperationName, activeInstances));
    }

    private ServiceInstanceOperation findActiveOperation(String operation, List<ServiceInstance> activeInstances) {
        if (CollectionUtils.isEmpty(activeInstances)) {
            return null;
        }
        List<ServiceInstanceOperation> operationList = activeInstances.get(0).getOperationList();
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

    private List<ServiceInstance> findActiveInstances(AppServiceDescriptor serviceDescriptor, String operationName) {
        List<ServiceInstance> newTargetInstanceList = Lists.newArrayList();
        List<ServiceInstance> targetActiveInstanceList = serviceDescriptor.getTargetActiveInstanceList();
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
package com.arextest.schedule.plan;

import com.arextest.schedule.model.AppServiceDescriptor;
import com.arextest.schedule.model.AppServiceOperationDescriptor;
import com.arextest.schedule.model.OperationTypeData;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.deploy.DeploymentVersion;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.model.deploy.ServiceInstanceOperation;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

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
    if (CollectionUtils.isEmpty(appServiceDescriptorList)) {
      return null;
    }
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

  public List<AppServiceDescriptor> filterAppServiceDescriptors(List<String> operationIds) {
    if (CollectionUtils.isEmpty(operationIds) || CollectionUtils.isEmpty(
        appServiceDescriptorList)) {
      return Collections.emptyList();
    }
    List<AppServiceDescriptor> filterAppServiceDescriptorList = new ArrayList<>(
        appServiceDescriptorList.size());
    for (AppServiceDescriptor appServiceDescriptor : appServiceDescriptorList) {
      if (null == appServiceDescriptor || CollectionUtils.isEmpty(
          appServiceDescriptor.getOperationList())) {
        continue;
      }
      List<AppServiceOperationDescriptor> operationDescriptorList = appServiceDescriptor.getOperationList();
      if (operationDescriptorList.stream().anyMatch(item -> operationIds.contains(item.getId()))) {
        filterAppServiceDescriptorList.add(appServiceDescriptor);
      }
    }
    return filterAppServiceDescriptorList;
  }

  public List<ServiceInstance> targetActiveInstance() {
    return getActiveInstance(AppServiceDescriptor::getTargetActiveInstanceList, true);
  }

  private List<ServiceInstance> getActiveInstance(
      Function<AppServiceDescriptor, List<ServiceInstance>> source, boolean targetInstance) {
    List<ServiceInstance> instanceList = Lists.newArrayList();
    for (AppServiceDescriptor descriptor : appServiceDescriptorList) {
      List<ServiceInstance> activeInstanceList =
          targetInstance ? descriptor.getTargetActiveInstanceList()
              : descriptor.getSourceActiveInstanceList();
      if (CollectionUtils.isEmpty(activeInstanceList)) {
        continue;
      }
      if (activeInstanceList.get(0) == null || StringUtils.isEmpty(
          activeInstanceList.get(0).getIp())) {
        continue;
      }
      instanceList.addAll(source.apply(descriptor));
      break;
    }
    return instanceList;
  }

  private ServiceInstance firstActiveInstance(
      Function<AppServiceDescriptor, List<ServiceInstance>> source) {
    if (CollectionUtils.isEmpty(appServiceDescriptorList)) {
      return null;
    }
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
    return getActiveInstance(AppServiceDescriptor::getSourceActiveInstanceList, false);
  }

  public ReplayActionItem toReplayAction(AppServiceOperationDescriptor operationDescriptor) {
    ReplayActionItem replayActionItem = new ReplayActionItem();
    fillReplayAction(replayActionItem, operationDescriptor);
    return replayActionItem;
  }

  public void fillReplayAction(ReplayActionItem replayActionItem,
      AppServiceOperationDescriptor operationDescriptor) {
    AppServiceDescriptor serviceDescriptor = operationDescriptor.getParent();
    replayActionItem.setAppId(serviceDescriptor.getAppId());
    final String operationName = operationDescriptor.getOperationName();
    replayActionItem.setTargetInstance(serviceDescriptor.getTargetActiveInstanceList());
    replayActionItem.setSourceInstance(serviceDescriptor.getSourceActiveInstanceList());
    replayActionItem.setOperationName(operationName);
    replayActionItem.setActionType(operationDescriptor.getOperationType());
    replayActionItem.setServiceKey(serviceDescriptor.getServiceKey());
    replayActionItem.setServiceName(serviceDescriptor.getServiceName());
    replayActionItem.setOperationId(operationDescriptor.getId());
    Set<String> operationTypes = operationDescriptor.getOperationTypes();
    List<OperationTypeData> list = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(operationTypes)) {
      for (String operationType : operationTypes) {
        list.add(new OperationTypeData(operationType));
      }
    }
    replayActionItem.setOperationTypes(list);
    if (CollectionUtils.isNotEmpty(serviceDescriptor.getTargetActiveInstanceList())) {
      replayActionItem.setMappedInstanceOperation(this.findActiveOperation(operationName,
          serviceDescriptor.getTargetActiveInstanceList().get(0)));
    }
  }

  private ServiceInstanceOperation findActiveOperation(String operation,
      ServiceInstance activeInstance) {
    if (activeInstance != null) {
      List<ServiceInstanceOperation> operationList = activeInstance.getOperationList();
      if (CollectionUtils.isEmpty(operationList)) {
        return null;
      }
      for (ServiceInstanceOperation serviceInstanceOperation : operationList) {
        if (StringUtils.equals(operation, serviceInstanceOperation.getName())) {
          return serviceInstanceOperation;
        }
      }
    }
    return null;
  }
}
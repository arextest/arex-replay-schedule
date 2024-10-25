package com.arextest.schedule.plan;

import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.model.AppServiceDescriptor;
import com.arextest.schedule.model.AppServiceOperationDescriptor;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author jmo
 * @since 2021/9/22
 */
@Component
public final class PlanContextCreator {

  @Resource
  private HttpWepServiceApiClient apiClient;
  @Value("${arex.api.config.applicationService.url}")
  private String applicationServiceUrl;

  public PlanContext createByAppId(String appId) {
    PlanContext planContext = new PlanContext();
    GenericResponseType responseType = apiClient.get(applicationServiceUrl,
        Collections.singletonMap("appId",
            appId),
        GenericResponseType.class);
    if (responseType == null) {
      return planContext;
    }
    planContext.setAppId(appId);
    List<AppServiceDescriptor> appServiceDescriptorList = responseType.getBody();
    if (CollectionUtils.isEmpty(appServiceDescriptorList)) {
      return planContext;
    }
    Map<String, List<AppServiceDescriptor>> serviceKeyAppServices = appServiceDescriptorList.stream()
        .collect(Collectors.groupingBy(AppServiceDescriptor::getServiceKey));
    List<AppServiceDescriptor> distinctAppServiceDescriptor = new ArrayList<>(
        appServiceDescriptorList.size());
    for (Map.Entry<String, List<AppServiceDescriptor>> appServiceDescriptor : serviceKeyAppServices.entrySet()) {
      List<AppServiceDescriptor> appServiceDescriptorValue = appServiceDescriptor.getValue();
      if (CollectionUtils.isNotEmpty(appServiceDescriptorValue)) {
        AppServiceDescriptor distinctAppService = appServiceDescriptorValue.get(0);
        setupOperationParent(distinctAppService);
        distinctAppServiceDescriptor.add(distinctAppService);
      }
    }
    planContext.setAppServiceDescriptorList(distinctAppServiceDescriptor);
    return planContext;
  }

  private void setupOperationParent(AppServiceDescriptor appServiceDescriptor) {
    List<AppServiceOperationDescriptor> operationList = appServiceDescriptor.getOperationList();
    if (CollectionUtils.isEmpty(operationList)) {
      return;
    }
    for (AppServiceOperationDescriptor operationDescriptor : operationList) {
      operationDescriptor.setParent(appServiceDescriptor);
    }
  }

  @Data
  private final static class GenericResponseType {

    private List<AppServiceDescriptor> body;
  }
}
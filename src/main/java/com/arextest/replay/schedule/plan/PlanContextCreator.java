package com.arextest.replay.schedule.plan;

import com.arextest.replay.schedule.client.HttpWepServiceApiClient;
import com.arextest.replay.schedule.model.AppServiceDescriptor;
import com.arextest.replay.schedule.model.AppServiceOperationDescriptor;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author jmo
 * @since 2021/9/22
 */
@Component
public final class PlanContextCreator {
    @Resource
    private HttpWepServiceApiClient apiClient;
    @Value("${arex.report.config.applicationService.url}")
    private String applicationServiceUrl;

    public PlanContext createByAppId(String appId) {
        PlanContext planContext = new PlanContext();
        GenericResponseType responseType = apiClient.get(applicationServiceUrl, Collections.singletonMap("appId",
                appId),
                GenericResponseType.class);
        if (responseType == null) {
            return planContext;
        }
        planContext.setAppId(appId);
        List<AppServiceDescriptor> appServiceDescriptorList = responseType.getBody();
        appServiceDescriptorList = addQmqDescriptor(appServiceDescriptorList, appId);
        if (CollectionUtils.isEmpty(appServiceDescriptorList)) {
            return planContext;
        }
        for (AppServiceDescriptor appServiceDescriptor : appServiceDescriptorList) {
            setupOperationParent(appServiceDescriptor);
        }
        planContext.setAppServiceDescriptorList(appServiceDescriptorList);
        return planContext;
    }

    @Data
    private final static class GenericResponseType {
        private List<AppServiceDescriptor> body;
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

    private List<AppServiceDescriptor> addQmqDescriptor(List<AppServiceDescriptor> descriptorList, String appId) {
        AppServiceDescriptor qmqDescriptor = new AppServiceDescriptor();
        qmqDescriptor.setId(null);
        qmqDescriptor.setAppId(appId);
        qmqDescriptor.setServiceName("QMQ");
        if (CollectionUtils.isEmpty(descriptorList)) {
            descriptorList = new ArrayList<>();
        }
        descriptorList.add(qmqDescriptor);
        return descriptorList;
    }
}

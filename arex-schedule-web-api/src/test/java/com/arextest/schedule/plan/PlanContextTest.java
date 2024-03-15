package com.arextest.schedule.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import com.arextest.schedule.model.AppServiceDescriptor;
import com.arextest.schedule.model.AppServiceOperationDescriptor;
import com.arextest.schedule.model.OperationTypeData;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.deploy.ServiceInstance;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PlanContextTest {
  @InjectMocks
  private PlanContext planContext;
  @Mock
  private AppServiceOperationDescriptor operationDescriptor;
  @Mock
  private ReplayActionItem replayActionItem;
  @Mock
  private AppServiceDescriptor serviceDescriptor;
  @Mock
  private OperationTypeData operationTypeData;
  @Mock
  private List<String> instanceList;

  @Test
  void testFillReplayAction() {
    MockitoAnnotations.openMocks(this);

    String appId = "test-app";
    String targetInstance = "test-target-instance";
    String operationName = "test-operation";
    String actionType = "SOAProvider";
    String serviceKey = "test-service-key";
    String serviceName = "test-service-name";
    String operationId = "test-operation-id";
    String operationType = "SOAProvider";
    ServiceInstance serviceInstance = new ServiceInstance();
    serviceInstance.setIp(targetInstance);

    when(operationDescriptor.getParent()).thenReturn(serviceDescriptor);
    when(serviceDescriptor.getAppId()).thenReturn(appId);
    when(serviceDescriptor.getServiceKey()).thenReturn(serviceKey);
    when(serviceDescriptor.getServiceName()).thenReturn(serviceName);
    when(serviceDescriptor.getTargetActiveInstanceList()).thenReturn(Collections.singletonList(serviceInstance));
    when(serviceDescriptor.getSourceActiveInstanceList()).thenReturn(Collections.singletonList(serviceInstance));
    when(operationDescriptor.getOperationName()).thenReturn(operationName);
    when(operationDescriptor.getOperationType()).thenReturn(actionType);
    when(operationDescriptor.getId()).thenReturn(operationId);
    when(operationDescriptor.getOperationTypes()).thenReturn(Collections.singleton(operationType));
    when(operationTypeData.getOperationType()).thenReturn(operationType);
    when(instanceList.get(0)).thenReturn(targetInstance);
    ReplayActionItem item = new ReplayActionItem();
    planContext.fillReplayAction(item, operationDescriptor);
    assertThat(item.getOperationTypes().get(0).getOperationType()).isEqualTo(operationType);
  }

  @Test
  void testOperationTypeData() {
    OperationTypeData data1 = new OperationTypeData("SOAProvider");
    OperationTypeData data2 = new OperationTypeData(1000, 100, "DubboProvider");
    OperationTypeData data3 = new OperationTypeData(0, 0, "KafkaProvider");

    assertThat(data1.getOperationType()).isEqualTo("SOAProvider");
    assertThat(data2.getLastRecordTime()).isEqualTo(1000);
    assertThat(data2.getTotalLoadedCount()).isEqualTo(100);
    assertThat(data2.getOperationType()).isEqualTo("DubboProvider");
  }

}
package com.arextest.schedule.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.arextest.common.config.DefaultApplicationConfig;
import com.arextest.schedule.common.RateLimiterFactory;
import com.arextest.schedule.common.SendSemaphoreLimiter;
import com.arextest.schedule.comparer.ComparisonWriter;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.model.*;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.progress.ProgressTracer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import com.arextest.schedule.sender.ReplaySenderFactory;
import com.arextest.schedule.sender.impl.DefaultHttpReplaySender;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.validation.constraints.AssertTrue;

class ReplayCaseTransmitServiceImplTest {
  @InjectMocks
  private ReplayCaseTransmitServiceImpl replayCaseTransmitServiceImpl;
  @Mock
  private ReplayActionCaseItemRepository replayActionCaseItemRepository;
  @Mock
  private ScheduledExecutorService compareScheduleExecutorService;
  @Mock
  private ExecutorService compareExecutorService;
  @Mock
  private DefaultApplicationConfig defaultConfig;
  @Mock
  private ReplayCompareService replayCompareService;
  @Mock
  private ComparisonWriter comparisonWriter;
  @Mock
  private ProgressTracer progressTracer;
  @Mock
  private ExecutorService sendExecutorService;
  @Mock
  private ExecutionStatus executionStatus;
  @Mock
  private ReplaySenderFactory senderFactory;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
  }
  @Test
  void testUpdateSendResult_WhenSendStatusTypeIsSuccess_SchedulesAsyncCompareTask() {
    ReplayActionCaseItem caseItem = new ReplayActionCaseItem();
    caseItem.setId("test");
    caseItem.setSourceResultId(null);
    caseItem.setTargetResultId(null);
    caseItem.setSendStatus(CaseSendStatusType.SUCCESS.getValue());
    ReplayActionItem parent = new ReplayActionItem();
    parent.setAppId("appId");
    caseItem.setParent(parent);
    when(defaultConfig.getConfigAsInt(parent.getAppId())).thenReturn(60);
    when(replayCompareService.compareCaseDistributable(caseItem)).thenReturn(true);
    replayCaseTransmitServiceImpl.updateSendResult(caseItem, CaseSendStatusType.SUCCESS);
    verify(replayActionCaseItemRepository).updateSendResult(caseItem);
    assertEquals(CaseSendStatusType.SUCCESS.getValue(), caseItem.getSendStatus());
  }

  @Test
  void testUpdateSendResultWithZeroDelaySeconds() {
    // Arrange
    ReplayActionCaseItem caseItem = new ReplayActionCaseItem();
    caseItem.setId("test");
    caseItem.setSourceResultId(null);
    caseItem.setTargetResultId(null);
    caseItem.setSendStatus(CaseSendStatusType.SUCCESS.getValue());
    ReplayActionItem parent = new ReplayActionItem();
    parent.setAppId("appId");
    caseItem.setParent(parent);
    when(defaultConfig.getConfigAsInt(parent.getAppId())).thenReturn(0);
    replayCaseTransmitServiceImpl.updateSendResult(caseItem, CaseSendStatusType.SUCCESS);
    assertEquals(CaseSendStatusType.SUCCESS.getValue(), caseItem.getSendStatus());
  }

  @Test
  void testUpdateSendResult_WhenSendStatusTypeIsNotSuccess_CallsDoSendFailedAsFinish() {
    ReplayActionCaseItem caseItem = new ReplayActionCaseItem();
    caseItem.setId("test");
    caseItem.setSourceResultId(null);
    caseItem.setTargetResultId(null);
    caseItem.setSendStatus(CaseSendStatusType.SUCCESS.getValue());
    when(comparisonWriter.writeIncomparable(caseItem, "error")).thenReturn(true);
    replayCaseTransmitServiceImpl.updateSendResult(caseItem, CaseSendStatusType.EXCEPTION_FAILED);
  }

  @Test
  void testDoExecute() {
    ReplayActionCaseItem replayActionCaseItem = new ReplayActionCaseItem();
    replayActionCaseItem.setId("6695e7e4d09d83326ef4fb55");
    replayActionCaseItem.setCaseType("caseType");
    ReplayActionItem actionItem = new ReplayActionItem();
    actionItem.setAppId("appId");
    replayActionCaseItem.setParent(actionItem);
    ServiceInstance targetServiceInstance = new ServiceInstance();
    PlanExecutionContext<?> planExecutionContext = new PlanExecutionContext<>();
    Map<ServiceInstance, List<ServiceInstance>> bindInstanceMap = new HashMap<>();
    List<ServiceInstance> sourceServiceInstanceList = Lists.list(new ServiceInstance());
    bindInstanceMap.put(targetServiceInstance, sourceServiceInstanceList);
    planExecutionContext.setBindInstanceMap(bindInstanceMap);
    CountDownLatch groupSentLatch = new CountDownLatch(1);
    SendSemaphoreLimiter sendSemaphoreLimiter = new SendSemaphoreLimiter(20, 1);

    Mockito.when(senderFactory.findReplaySender(replayActionCaseItem.getCaseType()))
            .thenReturn(new DefaultHttpReplaySender());

    ReflectionTestUtils.invokeMethod(replayCaseTransmitServiceImpl, "doExecute",
            replayActionCaseItem, targetServiceInstance, groupSentLatch,
            planExecutionContext, sendSemaphoreLimiter);

    Mockito.verify(sendExecutorService).execute(Mockito.any());
  }

  @Test
  void testSetServiceInstance() {
    ReplayActionCaseItem replayActionCaseItem = new ReplayActionCaseItem();
    replayActionCaseItem.setId("6695e7e4d09d83326ef4fb55");
    ServiceInstance targetServiceInstance = new ServiceInstance();
    PlanExecutionContext<?> planExecutionContext = new PlanExecutionContext<>();
    Map<ServiceInstance, List<ServiceInstance>> bindInstanceMap = new HashMap<>();
    List<ServiceInstance> sourceServiceInstanceList = Lists.list(new ServiceInstance());
    bindInstanceMap.put(targetServiceInstance, sourceServiceInstanceList);
    planExecutionContext.setBindInstanceMap(bindInstanceMap);

    ReflectionTestUtils.invokeMethod(replayCaseTransmitServiceImpl, "setServiceInstance",
            replayActionCaseItem, targetServiceInstance, planExecutionContext);

    assertNotNull(replayActionCaseItem.getTargetInstance());
    assertNotNull(replayActionCaseItem.getSourceInstance());

    replayActionCaseItem.setSourceInstance(null);
    planExecutionContext.setBindInstanceMap(null);
    ReflectionTestUtils.invokeMethod(replayCaseTransmitServiceImpl, "setServiceInstance",
            replayActionCaseItem, targetServiceInstance, planExecutionContext);
    assertNotNull(replayActionCaseItem.getTargetInstance());
    assertNull(replayActionCaseItem.getSourceInstance());
  }

  @Test
  void testDoDistribute() {
    ReplayActionItem actionItem = new ReplayActionItem();
    actionItem.setAppId("appId");
    List<ReplayActionCaseItem> replayActionCaseItemList = Lists.list();
    for (int i = 0; i < 1; i++) {
      ReplayActionCaseItem replayActionCaseItem = new ReplayActionCaseItem();
      replayActionCaseItem.setId(i + "");
      replayActionCaseItem.setCaseType("caseType");
      replayActionCaseItem.setParent(actionItem);
      replayActionCaseItemList.add(replayActionCaseItem);
    }
    ServiceInstance targetServiceInstance = new ServiceInstance();
    targetServiceInstance.setIp("127.0.0.1");
    actionItem.setTargetInstance(Lists.list(targetServiceInstance));

    ReplayPlan replayPlan = new ReplayPlan();
    replayPlan.setAppId("appId");
    replayPlan.setReplaySendMaxQps(1);
    replayPlan.setCaseTotalCount(100);
    replayPlan.setReplayActionItemList(Lists.list(actionItem));
    new RateLimiterFactory(1, 2, replayPlan);

    PlanExecutionContext<?> planExecutionContext = new PlanExecutionContext<>();
    planExecutionContext.setPlan(replayPlan);

    ArrayBlockingQueue<ReplayActionCaseItem> arrayBlockingQueue =
            new ArrayBlockingQueue<>(replayActionCaseItemList.size(), false, replayActionCaseItemList);
    CountDownLatch groupSentLatch = new CountDownLatch(1);

    Mockito.when(senderFactory.findReplaySender("caseType"))
            .thenReturn(new DefaultHttpReplaySender());
    ReflectionTestUtils.invokeMethod(replayCaseTransmitServiceImpl, "doDistribute", arrayBlockingQueue,
            targetServiceInstance, groupSentLatch, planExecutionContext);

    Mockito.verify(sendExecutorService).execute(Mockito.any());

    assert arrayBlockingQueue.isEmpty();
  }

}
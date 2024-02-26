package com.arextest.schedule.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.arextest.schedule.comparer.ComparisonWriter;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.model.CaseSendStatusType;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.progress.ProgressTracer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
  private DefaultConfigProviderImpl defaultConfigProviderImpl;
  @Mock
  private ReplayCompareService replayCompareService;
  @Mock
  private ComparisonWriter comparisonWriter;
  @Mock
  private ProgressTracer progressTracer;
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
    when(defaultConfigProviderImpl.getCompareDelaySeconds(parent.getAppId())).thenReturn(60);
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
    when(defaultConfigProviderImpl.getCompareDelaySeconds(parent.getAppId())).thenReturn(0);
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

}
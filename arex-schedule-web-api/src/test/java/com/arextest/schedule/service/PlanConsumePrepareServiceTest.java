package com.arextest.schedule.service;


import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanActionRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanRepository;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.plan.PlanContextCreator;
import com.arextest.schedule.planexecution.PlanExecutionContextProvider;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.service.noise.ReplayNoiseIdentify;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

class PlanConsumePrepareServiceTest extends TestBase {

  @InjectMocks
  PlanConsumePrepareService service;

  @Mock
  private MetricService metricService;
  @Mock
  private ReplayPlanRepository replayPlanRepository;
  @Mock
  private ReplayReportService replayReportService;
  @Mock
  private ReplayCaseRemoteLoadService caseRemoteLoadService;
  @Mock
  private ReplayActionItemPreprocessService replayActionItemPreprocessService;
  @Mock
  private PlanExecutionContextProvider planExecutionContextProvider;
  @Mock
  private ProgressEvent progressEvent;
  @Mock
  private ReplayActionCaseItemRepository replayActionCaseItemRepository;
  @Mock
  private ReplayPlanActionRepository replayPlanActionRepository;
  @Mock
  private PlanContextCreator planContextCreator;
  @Mock
  private DeployedEnvironmentService deployedEnvironmentService;
  @Mock
  private ExecutorService rerunPrepareExecutorService;
  @Mock
  private ReplayNoiseIdentify replayNoiseIdentify;

  @Test
  void simple() {
    ReplayPlan plan = preparePlan();

    // base simple test
    service.prepareRunData(plan);

    // test simple case save
    Mockito
        .when(caseRemoteLoadService.pagingLoad(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(),
            Mockito.anyInt(), Mockito.any()))
        .thenReturn(Collections.singletonList(prepareBaseCase()));
    service.prepareRunData(plan);
    Assertions.assertEquals(1, plan.getCaseTotalCount());

    // test case saved in previous run
    plan.getReplayActionItemList().get(0).setReplayStatus(ReplayStatusType.CASE_LOADED.getValue());
    service.prepareRunData(plan);
    Assertions.assertEquals(1, plan.getCaseTotalCount());
  }

  @Test
  void caseLoaded() {
    ReplayPlan plan = preparePlan();

    // test case saved in previous run
    ReplayActionItem action = plan.getReplayActionItemList().get(0);
    action.setReplayStatus(ReplayStatusType.CASE_LOADED.getValue());
    action.setReplayCaseCount(1);
    service.prepareRunData(plan);
    Assertions.assertEquals(1, plan.getCaseTotalCount());
  }
}
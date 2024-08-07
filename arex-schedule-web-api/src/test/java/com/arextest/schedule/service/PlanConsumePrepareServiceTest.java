package com.arextest.schedule.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.arextest.common.config.DefaultApplicationConfig;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.model.CaseProviderEnum;
import com.arextest.schedule.model.OperationTypeData;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.planexecution.PlanExecutionContextProvider;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PlanConsumePrepareServiceTest {
  @InjectMocks
  private PlanConsumePrepareService planConsumePrepareService;

  @Mock
  private ReplayActionItem replayActionItem;

  @Mock
  private ReplayPlan replayPlan;

  @Mock
  private ReplayCaseRemoteLoadService caseRemoteLoadService;

  @Mock
  private ReplayActionCaseItemRepository replayActionCaseItemRepository;
  @Mock
  private PlanExecutionContextProvider planExecutionContextProvider;
  @Mock
  private DefaultApplicationConfig defaultApplicationConfig;
  private static final String CASE_COUNT_LIMIT_KEY = ".case.count.limit.";
  private static final String CASE_COUNT_LIMIT_APPS_KEY = "use.case.count.limit.apps";
  private static final String ALL = "ALL";

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    List<OperationTypeData> operationTypes = new ArrayList<>();
    operationTypes.add(new OperationTypeData(0, 0, "SOAProvider"));
    when(replayActionItem.getOperationTypes()).thenReturn(operationTypes);

    when(replayPlan.getCaseSourceFrom()).thenReturn(new Date(1710405850000L));
    when(replayPlan.getCaseSourceTo()).thenReturn(new Date(1710416650396L));
    when(replayPlan.getCaseCountLimit()).thenReturn(10);
    when(replayActionItem.getParent()).thenReturn(replayPlan);
  }

  @Test
  void testDoPagingLoadCaseSave() {
    when(caseRemoteLoadService.pagingLoad(
        any(Long.class), any(Long.class), any(ReplayActionItem.class),
        any(Integer.class), any(String.class), any(String.class)))
        .thenReturn(new ArrayList<>());
    when(defaultApplicationConfig.getConfigAsString(CASE_COUNT_LIMIT_APPS_KEY, ALL))
        .thenReturn(null);
    int result = planConsumePrepareService.loadCasesByProvider(replayActionItem,
        CaseProviderEnum.ROLLING);
    assertEquals(0, result);
  }

  @Test
  void testDoPagingLoadCaseSave_notHaveCase() {
    List<ReplayActionCaseItem> list = new ArrayList<>();
    for (int i=0; i < 10; i++) {
      ReplayActionCaseItem replayActionCaseItem = new ReplayActionCaseItem();
      replayActionCaseItem.setRecordTime(1710409450000L);
      list.add(replayActionCaseItem);
    }
    when(caseRemoteLoadService.pagingLoad(
        any(Long.class), any(Long.class), any(ReplayActionItem.class),
        any(Integer.class), any(String.class), any(String.class)))
        .thenReturn(list);

    when(defaultApplicationConfig.getConfigAsString(CASE_COUNT_LIMIT_APPS_KEY, ALL))
        .thenReturn(ALL);
    when(defaultApplicationConfig.getConfigAsString(
        ArgumentMatchers.contains(
            CaseProviderEnum.ROLLING.getName() + CASE_COUNT_LIMIT_KEY + 0)))
        .thenReturn("0");

    int result = planConsumePrepareService.loadCasesByProvider(replayActionItem,
        CaseProviderEnum.ROLLING);
    assertEquals(0, result);
  }

  @Test
  void testDoPagingLoadCaseSave_defaultCaseCount() {
    List<ReplayActionCaseItem> list = new ArrayList<>();
    for (int i=0; i < 10; i++) {
      ReplayActionCaseItem replayActionCaseItem = new ReplayActionCaseItem();
      replayActionCaseItem.setRecordTime(1710409450000L);
      list.add(replayActionCaseItem);
    }
    when(caseRemoteLoadService.pagingLoad(
        any(Long.class), any(Long.class), any(ReplayActionItem.class),
        any(Integer.class), any(String.class), any(String.class)))
        .thenReturn(list);

    when(defaultApplicationConfig.getConfigAsString(CASE_COUNT_LIMIT_APPS_KEY, ALL))
        .thenReturn(null);

    int result = planConsumePrepareService.loadCasesByProvider(replayActionItem,
        CaseProviderEnum.ROLLING);
    assertEquals(10, result);
  }

//
//  @Test
//  public void testGetAllOperationTypeSize() {
//    int result = planConsumePrepareService.getAllOperationTypeSize(
//        replayActionItem, "providerName", new OperationTypeData(0, 1000, "SOAProvider"));
//    assertEquals(1000, result);
//  }

}
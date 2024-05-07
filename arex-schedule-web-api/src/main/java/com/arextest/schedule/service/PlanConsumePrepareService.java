package com.arextest.schedule.service;

import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanActionRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanRepository;
import com.arextest.schedule.model.AppServiceDescriptor;
import com.arextest.schedule.model.AppServiceOperationDescriptor;
import com.arextest.schedule.model.CaseSendStatusType;
import com.arextest.schedule.model.CompareProcessStatusType;
import com.arextest.schedule.model.LogType;
import com.arextest.schedule.model.OperationTypeData;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.model.plan.BuildReplayPlanType;
import com.arextest.schedule.plan.PlanContext;
import com.arextest.schedule.plan.PlanContextCreator;
import com.arextest.schedule.planexecution.PlanExecutionContextProvider;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.service.noise.ReplayNoiseIdentify;
import com.arextest.schedule.utils.ReplayParentBinder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * Created by Qzmo on 2023/7/5
 */
@Service
@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class PlanConsumePrepareService {

  @Resource
  private MetricService metricService;
  @Resource
  private ReplayPlanRepository replayPlanRepository;
  @Resource
  private ReplayReportService replayReportService;
  @Resource
  private ReplayCaseRemoteLoadService caseRemoteLoadService;
  @Resource
  private ReplayActionItemPreprocessService replayActionItemPreprocessService;
  @Resource
  private PlanExecutionContextProvider planExecutionContextProvider;
  @Resource
  private ProgressEvent progressEvent;
  @Resource
  private ReplayActionCaseItemRepository replayActionCaseItemRepository;
  @Resource
  private ReplayPlanActionRepository replayPlanActionRepository;
  @Resource
  private PlanContextCreator planContextCreator;
  @Resource
  private DeployedEnvironmentService deployedEnvironmentService;
  @Resource
  private ExecutorService rerunPrepareExecutorService;
  @Resource
  private ReplayNoiseIdentify replayNoiseIdentify;

  public int prepareRunData(ReplayPlan replayPlan) {
    if (replayPlan.getPlanCreateMillis() == 0) {
      LOGGER.warn("The plan create time is null, planId:{}, appId:{}", replayPlan.getId(),
          replayPlan.getAppId());
    } else {
      metricService.recordTimeEvent(LogType.PLAN_EXECUTION_DELAY.getValue(), replayPlan.getId(),
          replayPlan.getAppId(), null,
          System.currentTimeMillis() - replayPlan.getPlanCreateMillis());
    }
    int planSavedCaseSize = saveAllActionCase(replayPlan.getReplayActionItemList());
    if (!replayPlan.isReRun() && planSavedCaseSize != replayPlan.getCaseTotalCount()) {
      LOGGER.info("update the plan TotalCount, plan id:{} ,appId: {} , size: {} -> {}",
          replayPlan.getId(),
          replayPlan.getAppId(), replayPlan.getCaseTotalCount(), planSavedCaseSize);
      replayPlan.setCaseTotalCount(planSavedCaseSize);
      replayPlanRepository.updateCaseTotal(replayPlan.getId(), planSavedCaseSize);
      replayPlan.setInitReportItem(true);
      replayReportService.initReportInfo(replayPlan);
    }
    return planSavedCaseSize;
  }

  private int saveAllActionCase(List<ReplayActionItem> replayActionItemList) {
    int planSavedCaseSize = 0;
    for (ReplayActionItem replayActionItem : replayActionItemList) {
      if (replayActionItem.getReplayStatus() != ReplayStatusType.INIT.getValue()) {
        planSavedCaseSize += replayActionItem.getReplayCaseCount();
        continue;
      }
      int preloaded = replayActionItem.getReplayCaseCount();
      int actionSavedCount = streamingCaseItemSave(replayActionItem);

      planSavedCaseSize += actionSavedCount;
      if (!replayActionItem.getParent().isReRun() && preloaded != actionSavedCount) {
        replayActionItem.setReplayCaseCount(actionSavedCount);
        LOGGER.warn("The saved case size of actionItem not equals, preloaded size:{},saved size:{}",
            preloaded,
            actionSavedCount);
      }
      progressEvent.onActionCaseLoaded(replayActionItem);
    }
    return planSavedCaseSize;
  }

  private int streamingCaseItemSave(ReplayActionItem replayActionItem) {
    List<ReplayActionCaseItem> caseItemList = replayActionItem.getCaseItemList();
    int size;
    if (CollectionUtils.isNotEmpty(caseItemList)) {
      size = doFixedCaseSave(replayActionItem);
    } else {
      if (replayActionItem.getParent().getReplayPlanType()
          == BuildReplayPlanType.MIXED.getValue()) {
        size = doPagingLoadCaseSave(replayActionItem, CommonConstant.AUTO_PINED) +
            doPagingLoadCaseSave(replayActionItem, CommonConstant.ROLLING);
      } else {
        size = doPagingLoadCaseSave(replayActionItem, CommonConstant.ROLLING);
      }
    }
    return size;
  }

  private void caseItemPostProcess(List<ReplayActionCaseItem> caseItemList) {
    // to provide necessary fields into case item for context to consume when sending
    planExecutionContextProvider.injectContextIntoCase(caseItemList);
  }

  /**
   * Paging query storage's recording data. if caseCountLimit > CommonConstant.MAX_PAGE_SIZE,
   * Calculate the latest pageSize and recycle pagination queries
   * <p>
   * else if caseCountLimit < CommonConstant.MAX_PAGE_SIZE or recording data size < request page
   * size, Only need to query once by page
   */
  public int doPagingLoadCaseSave(ReplayActionItem replayActionItem, String providerName) {
    List<OperationTypeData> operationTypes = replayActionItem.getOperationTypes();
    int totalCount = 0;

    if (CollectionUtils.isEmpty(operationTypes)) {
      return totalCount;
    }

    for (OperationTypeData operationTypeData : operationTypes) {
      totalCount += loadCaseWithOperationType(replayActionItem, providerName, operationTypeData);
    }
    return totalCount;
  }

  /**
   * Find data by paging according to operationType
   * ps: providerName: Rolling, operationType: DubboProvider
   */
  private int loadCaseWithOperationType(ReplayActionItem replayActionItem, String providerName,
      OperationTypeData operationTypeData) {
    final ReplayPlan replayPlan = replayActionItem.getParent();
    long beginTimeMills = replayPlan.getCaseSourceFrom().getTime();
    // need all auto pined cases
    if (providerName.equals(CommonConstant.AUTO_PINED)) {
      beginTimeMills = 0;
    }

    long endTimeMills = operationTypeData.getLastRecordTime();
    if (endTimeMills == 0) {
      endTimeMills = replayPlan.getCaseSourceTo().getTime();
    }
    int caseCountLimit = replayPlan.getCaseCountLimit();
    int pageSize = Math.min(caseCountLimit, CommonConstant.MAX_PAGE_SIZE);

    // The task is pulled up to obtain the recorded case
    int count = (int) operationTypeData.getTotalLoadedCount();

    if (count == caseCountLimit) {
      return count;
    }

    while (beginTimeMills < endTimeMills) {
      List<ReplayActionCaseItem> caseItemList = caseRemoteLoadService.pagingLoad(beginTimeMills,
          endTimeMills, replayActionItem, caseCountLimit - count,
          providerName, operationTypeData.getOperationType());
      if (CollectionUtils.isEmpty(caseItemList)) {
        break;
      }
      caseItemPostProcess(caseItemList);
      ReplayParentBinder.setupCaseItemParent(caseItemList, replayActionItem);
      count += caseItemList.size();
      endTimeMills = caseItemList.get(caseItemList.size() - 1).getRecordTime();
      replayActionCaseItemRepository.save(caseItemList);
      if (count >= caseCountLimit || caseItemList.size() < pageSize) {
        break;
      }
    }
    return count;
  }

  private int doFixedCaseSave(ReplayActionItem replayActionItem) {
    List<ReplayActionCaseItem> caseItemList = replayActionItem.getCaseItemList();
    int size = 0;
    for (int i = 0; i < caseItemList.size(); i++) {
      ReplayActionCaseItem caseItem = caseItemList.get(i);
      Set<String> operationTypes = new HashSet<>();
      if (CollectionUtils.isNotEmpty(caseItem.getParent().getOperationTypes())) {
        operationTypes = caseItem.getParent().getOperationTypes().stream().map(OperationTypeData::getOperationType).collect(
            Collectors.toSet());
      }
      ReplayActionCaseItem viewReplay = caseRemoteLoadService.viewReplayLoad(caseItem,
          operationTypes);
      if (viewReplay == null) {
        caseItem.setSendStatus(CaseSendStatusType.REPLAY_CASE_NOT_FOUND.getValue());
        caseItem.setSourceResultId(StringUtils.EMPTY);
        caseItem.setTargetResultId(StringUtils.EMPTY);
      } else {
        caseItemList.set(i, viewReplay);
        size++;
      }
    }
    ReplayParentBinder.setupCaseItemParent(caseItemList, replayActionItem);
    caseItemPostProcess(caseItemList);
    if (!replayActionItem.getParent().isReRun()) {
      replayActionCaseItemRepository.save(caseItemList);
    }
    return size;
  }

  // region rerun plan
  public void updateFailedActionAndCase(ReplayPlan replayPlan,
      List<ReplayActionCaseItem> failedCaseList) {
    List<ReplayActionItem> replayActionItems = replayPlanActionRepository.queryPlanActionList(
        replayPlan.getId());

    Map<String, List<ReplayActionCaseItem>> failedCaseMap = failedCaseList.stream()
        .peek(caseItem -> {
          caseItem.setSendStatus(CaseSendStatusType.WAIT_HANDLING.getValue());
          caseItem.setCompareStatus(CompareProcessStatusType.WAIT_HANDLING.getValue());
        }).collect(Collectors.groupingBy(ReplayActionCaseItem::getPlanItemId));

    List<ReplayActionItem> failedActionList = replayActionItems.stream()
        .filter(actionItem -> CollectionUtils.isNotEmpty(failedCaseMap.get(actionItem.getId())))
        .peek(actionItem -> {
          actionItem.setParent(replayPlan);
          actionItem.setCaseItemList(failedCaseMap.get(actionItem.getId()));
          actionItem.setReplayStatus(ReplayStatusType.INIT.getValue());
          actionItem.setRerunCaseCount(actionItem.getCaseItemList().size());
          ReplayParentBinder.setupCaseItemParent(actionItem.getCaseItemList(), actionItem);
        }).collect(Collectors.toList());
    replayPlan.setReplayActionItemList(failedActionList);

    doResumeOperationDescriptor(replayPlan);
    // filter actionItem by appId and fill exclusionOperationConfig
    List<String> excludedActionIds = replayActionItemPreprocessService.filterActionItem(
        replayPlan.getReplayActionItemList(), replayPlan.getAppId());
    List<String> excludedCaseIds = failedCaseMap.entrySet().stream()
        .filter(entry -> excludedActionIds.contains(entry.getKey()))
        .flatMap(entry -> entry.getValue().stream()).map(ReplayActionCaseItem::getId)
        .collect(Collectors.toList());

    int caseRerunCount = replayPlan.getReplayActionItemList().stream()
        .mapToInt(ReplayActionItem::getRerunCaseCount).sum();
    replayPlan.setCaseRerunCount(caseRerunCount);
    replayPlan.setCaseTotalCount(replayPlan.getCaseTotalCount() - excludedCaseIds.size());

    Set<String> availableActionIds =
        replayPlan.getReplayActionItemList().stream().map(ReplayActionItem::getId)
            .collect(Collectors.toSet());
    // After filter actionItem, update related ReplayActionCaseItems
    Map<String, List<String>> actionIdAndRecordIdsMap =
        failedCaseMap.entrySet().stream()
            .filter(entry -> availableActionIds.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream()
                .map(ReplayActionCaseItem::getRecordId).collect(Collectors.toList())));
    List<ReplayActionCaseItem> newFailedCaseList = failedCaseList.stream()
        .filter(replayActionCaseItem -> availableActionIds.contains(
            replayActionCaseItem.getPlanItemId()))
        .collect(Collectors.toList());

    CompletableFuture removeRecordsAndScenesTask = CompletableFuture.runAsync(
        () -> replayReportService.removeRecordsAndScenes(actionIdAndRecordIdsMap),
        rerunPrepareExecutorService);
    CompletableFuture removeErrorMsgTask = CompletableFuture.runAsync(
        () -> replayReportService.removeErrorMsg(replayPlan.getId(),
            new ArrayList<>(actionIdAndRecordIdsMap.keySet())),
        rerunPrepareExecutorService);
    // XXX: Whether batch update actionCaseItem status is redundant, rerun in doFixedCaseSave has already
    // implemented this processing
    CompletableFuture batchUpdateStatusTask = CompletableFuture.runAsync(
        () -> replayActionCaseItemRepository.batchUpdateStatus(newFailedCaseList),
        rerunPrepareExecutorService);
    CompletableFuture<Void> noiseAnalysisRecover = CompletableFuture.runAsync(
        () -> replayNoiseIdentify.rerunNoiseAnalysisRecovery(replayPlan.getReplayActionItemList()),
        rerunPrepareExecutorService);

    // remove excluded action and case
    CompletableFuture<Void> updateReportTask = CompletableFuture.runAsync(
        () -> replayReportService.updateReportInfo(replayPlan));
    CompletableFuture<Void> deletePlanItemStatisticsTask = CompletableFuture.runAsync(
        () -> replayReportService.deletePlanItemStatistics(replayPlan.getId(), excludedActionIds));
    CompletableFuture<Void> deleteRunDetailsTask = CompletableFuture.runAsync(
        () -> replayActionCaseItemRepository.deleteExcludedCases(replayPlan.getId(), excludedActionIds));

    CompletableFuture.allOf(removeRecordsAndScenesTask, batchUpdateStatusTask, noiseAnalysisRecover,
        removeErrorMsgTask, updateReportTask, deletePlanItemStatisticsTask, deleteRunDetailsTask).join();
  }

  public void doResumeOperationDescriptor(ReplayPlan replayPlan) {
    PlanContext planContext = planContextCreator.createByAppId(replayPlan.getAppId());
    AppServiceOperationDescriptor operationDescriptor;
    for (ReplayActionItem actionItem : replayPlan.getReplayActionItemList()) {
      operationDescriptor = planContext.findAppServiceOperationDescriptor(
          actionItem.getOperationId());
      if (operationDescriptor == null) {
        LOGGER.warn("skip resume when the plan operationDescriptor not found, action id: {} ,",
            actionItem.getId());
        continue;
      }
      AppServiceDescriptor appServiceDescriptor = operationDescriptor.getParent();
      List<ServiceInstance> activeInstanceList =
          deployedEnvironmentService.getActiveInstanceList(appServiceDescriptor,
              replayPlan.getTargetEnv());
      appServiceDescriptor.setTargetActiveInstanceList(activeInstanceList);

      planContext.fillReplayAction(actionItem, operationDescriptor);
      replayPlan.setMinInstanceCount(planContext.determineMinInstanceCount());
    }
  }
  // endregion
}

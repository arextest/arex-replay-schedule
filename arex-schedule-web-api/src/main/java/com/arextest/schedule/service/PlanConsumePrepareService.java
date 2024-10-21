package com.arextest.schedule.service;

import com.arextest.common.config.DefaultApplicationConfig;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanActionRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanRepository;
import com.arextest.schedule.model.AppServiceDescriptor;
import com.arextest.schedule.model.AppServiceOperationDescriptor;
import com.arextest.schedule.model.CaseProviderEnum;
import com.arextest.schedule.model.CaseSendStatusType;
import com.arextest.schedule.model.CompareProcessStatusType;
import com.arextest.schedule.model.LogType;
import com.arextest.schedule.model.OperationTypeData;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.model.deploy.ServiceInstance;
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
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

/**
 * Created by Qzmo on 2023/7/5
 */
@Service
@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class PlanConsumePrepareService {
  private static final String CASE_COUNT_LIMIT_KEY = ".case.count.limit.";
  private static final String CASE_COUNT_LIMIT_APPS_KEY = "use.case.count.limit.apps";
  private static final String NO_TIME_LIMIT_KEY = "no.time.limit.";
  private static final String ALL = "ALL";
  private static final int ZERO_CASE_COUNT_LIMIT = 0;
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
  @Resource
  private ReplayStorageService replayStorageService;
  @Resource
  private DefaultApplicationConfig defaultConfig;

  public int preparePlan(ReplayPlan replayPlan) {
    if (replayPlan.getPlanCreateMillis() == 0) {
      LOGGER.warn("The plan create time is null, planId:{}, appId:{}", replayPlan.getId(),
          replayPlan.getAppId());
    } else {
      metricService.recordTimeEvent(LogType.PLAN_EXECUTION_DELAY.getValue(), replayPlan.getId(),
          replayPlan.getAppId(), null,
          System.currentTimeMillis() - replayPlan.getPlanCreateMillis());
    }
    replayStorageService.clearReplayScenes(replayPlan.getAppId());
    int planSavedCaseSize = prepareAllActions(replayPlan.getReplayActionItemList());
    if (!replayPlan.isReRun() && planSavedCaseSize != 0) {
      replayPlan.setCaseTotalCount(planSavedCaseSize);
      replayPlanRepository.updateCaseTotal(replayPlan.getId(), planSavedCaseSize);
      replayPlan.setInitReportItem(true);
      replayReportService.initReportInfo(replayPlan);
    }
    return planSavedCaseSize;
  }

  private int prepareAllActions(List<ReplayActionItem> replayActionItems) {
    int planLoadSize = 0;
    for (ReplayActionItem action : replayActionItems) {
      int actionLoadSize = 0;

      if (action.getReplayStatus() != ReplayStatusType.INIT.getValue()) {
        planLoadSize += action.getReplayCaseCount();
        continue;
      }

      if (!CollectionUtils.isEmpty(action.getCaseItemList())) {
        actionLoadSize += loadPinnedCases(action);
      } else {
        actionLoadSize += loadCasesByProvider(action, CaseProviderEnum.AUTO_PINNED);
        actionLoadSize += loadCasesByProvider(action, CaseProviderEnum.ROLLING);
      }
      planLoadSize += actionLoadSize;
      action.setReplayCaseCount(actionLoadSize);
      progressEvent.onActionCaseLoaded(action);
    }
    return planLoadSize;
  }

  private void caseItemPostProcess(List<ReplayActionCaseItem> cases, CaseProviderEnum provider) {
    if (CollectionUtils.isEmpty(cases)) {
      return;
    }

    cases.forEach(caseItem -> {
      caseItem.setCaseProviderCode(provider.getCode());
    });
    // to provide necessary fields into case item for context to consume when sending
    planExecutionContextProvider.injectContextIntoCase(cases);
  }

  /**
   * Paging query storage's recording data. if caseCountLimit > CommonConstant.MAX_PAGE_SIZE,
   * Calculate the latest pageSize and recycle pagination queries
   * else if caseCountLimit < CommonConstant.MAX_PAGE_SIZE or recording data size < request page
   * size, Only need to query once by page
   */
  public int loadCasesByProvider(ReplayActionItem replayActionItem, CaseProviderEnum provider) {
    List<OperationTypeData> operationTypes = replayActionItem.getOperationTypes();
    int totalCount = 0;

    if (CollectionUtils.isEmpty(operationTypes)) {
      return totalCount;
    }

    for (OperationTypeData operationTypeData : operationTypes) {
      totalCount += loadCaseWithOperationType(replayActionItem, provider, operationTypeData);
    }
    return totalCount;
  }

  /**
   * Find data by paging according to operationType
   * ps: providerName: Rolling, operationType: DubboProvider
   */
  private int loadCaseWithOperationType(ReplayActionItem replayActionItem, CaseProviderEnum provider,
      OperationTypeData operationTypeData) {
    final ReplayPlan replayPlan = replayActionItem.getParent();

    // Get the limit on the number of replay cases
    int caseCountLimit = getCaseCountLimit(replayPlan.getAppId(),
        replayPlan.getReplayPlanType(), provider.getName(), replayPlan.getCaseCountLimit());
    if (caseCountLimit == ZERO_CASE_COUNT_LIMIT) {
      return ZERO_CASE_COUNT_LIMIT;
    }

    // Rolling data needs to be judged whether to pull all
    long beginTimeMills = getBeginTimeMills(replayPlan.getReplayPlanType(),
        replayPlan.getCaseSourceFrom().getTime(), provider);
    long endTimeMills = getEndTimeMills(replayPlan.getCaseSourceTo().getTime(),
        operationTypeData.getLastRecordTime());

    int pageSize = Math.min(caseCountLimit, CommonConstant.MAX_PAGE_SIZE);

    // The task is pulled up to obtain the recorded case
    int count = (int) operationTypeData.getTotalLoadedCount();

    if (count == caseCountLimit) {
      return count;
    }

    while (beginTimeMills < endTimeMills) {
      List<ReplayActionCaseItem> caseItemList = caseRemoteLoadService.pagingLoad(beginTimeMills,
          endTimeMills, replayActionItem, caseCountLimit - count,
          provider.getName(), operationTypeData.getOperationType());
      if (CollectionUtils.isEmpty(caseItemList)) {
        break;
      }
      ReplayParentBinder.setupCaseItemParent(caseItemList, replayActionItem);
      caseItemPostProcess(caseItemList, provider);
      count += caseItemList.size();
      endTimeMills = caseItemList.get(caseItemList.size() - 1).getRecordTime();
      replayActionCaseItemRepository.save(caseItemList);
      if (count >= caseCountLimit || caseItemList.size() < pageSize) {
        break;
      }
    }
    return count;
  }

  /**
   * Get the limit on the number of replay cases
   * @param appId
   * @param replayPlanType
   * @param providerName
   * @param caseCountLimit
   * @return
   */
  private int getCaseCountLimit(String appId, int replayPlanType,
       String providerName, int caseCountLimit) {
    String configApps = defaultConfig.getConfigAsString(CASE_COUNT_LIMIT_APPS_KEY, ALL);
    // If the app list in the configuration contains all apps or contains currently scheduled apps
    if (StringUtils.equalsIgnoreCase(configApps, ALL)
        || StringUtils.contains(configApps, appId)) {
      // Get the use case number limit from configuration
      String limitValue = defaultConfig.getConfigAsString(
          buildLimitKey(replayPlanType, providerName));
      // If no use case limit is set in the configuration, the use case limit in the plan is used
      if (StringUtils.isBlank(limitValue)) {
        return caseCountLimit;
      }

      return NumberUtils.toInt(limitValue);
    }

    return caseCountLimit;
  }

  private String buildLimitKey(int replayPlanType, String providerName) {
    return providerName + CASE_COUNT_LIMIT_KEY + replayPlanType;
  }

  /**
   * Determine the replay start time of rolling cases through configuration
   * @param replayPlanType
   * @param caseSourceFrom
   * @param provider
   * @return
   */
  private long getBeginTimeMills(int replayPlanType, long caseSourceFrom, CaseProviderEnum provider) {
    // need all auto pined or rolling cases
    if (provider.equals(CaseProviderEnum.AUTO_PINNED) ||
        (provider.equals(CaseProviderEnum.ROLLING) &&
            defaultConfig.getConfigAsBoolean(NO_TIME_LIMIT_KEY + replayPlanType,
                false))) {
      return 0;
    }
    return caseSourceFrom;
  }

  /**
   * Determine the replay end time of rolling cases through configuration
   * @param caseSourceTo
   * @param lastRecordTime
   * @return
   */
  private long getEndTimeMills(long caseSourceTo, long lastRecordTime) {
    // Job wake-up time
    long endTimeMills = lastRecordTime;
    if (endTimeMills == 0) {
      endTimeMills = caseSourceTo;
    }
    return endTimeMills;
  }

  private int loadPinnedCases(ReplayActionItem replayActionItem) {
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
    caseItemPostProcess(caseItemList, CaseProviderEnum.PINNED);
    replayActionCaseItemRepository.save(caseItemList);
    replayActionItem.setReplayCaseCount(size);
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
          caseItem.setTargetResultId(Strings.EMPTY);
          caseItem.setSourceResultId(Strings.EMPTY);
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
    setHost(replayPlan);
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

    LOGGER.info("failed actionIdList:{}", availableActionIds);

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
        () -> replayReportService.updateReportInfo(replayPlan), rerunPrepareExecutorService);
    CompletableFuture<Void> deletePlanItemStatisticsTask = CompletableFuture.runAsync(
        () -> replayReportService.deletePlanItemStatistics(replayPlan.getId(), excludedActionIds),
        rerunPrepareExecutorService);
    CompletableFuture<Void> deleteRunDetailsTask = CompletableFuture.runAsync(
        () -> replayActionCaseItemRepository.deleteExcludedCases(replayPlan.getId(),
            excludedActionIds),
        rerunPrepareExecutorService);

    CompletableFuture.allOf(removeRecordsAndScenesTask, batchUpdateStatusTask, noiseAnalysisRecover,
        removeErrorMsgTask, updateReportTask, deletePlanItemStatisticsTask, deleteRunDetailsTask).join();
  }

  private void setHost(ReplayPlan replayPlan) {
    if (replayPlan == null || CollectionUtils.isEmpty(replayPlan.getReplayActionItemList()) ||
        replayPlan.getReplayActionItemList().get(0) == null || CollectionUtils.isEmpty(
        replayPlan.getReplayActionItemList().get(0).getTargetInstance())) {
      return;
    }

    List<ServiceInstance> targetInstance = replayPlan.getReplayActionItemList().get(0).getTargetInstance();
    String targetHost = targetInstance.stream().map(ServiceInstance::getIp).distinct().collect(Collectors.joining(","));
    replayPlan.setTargetHost(targetHost);
    List<ServiceInstance> serviceInstances = replayPlan.getReplayActionItemList().get(0).getSourceInstance();
    if (CollectionUtils.isNotEmpty(serviceInstances)) {
      String sourceHost = serviceInstances.stream().map(ServiceInstance::getIp).distinct()
          .collect(Collectors.joining(","));
      replayPlan.setSourceHost(sourceHost);
    }
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

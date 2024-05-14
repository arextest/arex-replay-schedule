package com.arextest.schedule.service;

import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanRepository;
import com.arextest.schedule.model.CaseSendStatusType;
import com.arextest.schedule.model.LogType;
import com.arextest.schedule.model.OperationTypeData;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.model.plan.BuildReplayPlanType;
import com.arextest.schedule.planexecution.PlanExecutionContextProvider;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.utils.ReplayParentBinder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
  private PlanExecutionContextProvider planExecutionContextProvider;
  @Resource
  private ProgressEvent progressEvent;
  @Resource
  private ReplayActionCaseItemRepository replayActionCaseItemRepository;

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
}

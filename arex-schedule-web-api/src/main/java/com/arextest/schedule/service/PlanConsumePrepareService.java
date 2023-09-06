package com.arextest.schedule.service;

import com.arextest.schedule.bizlog.BizLogger;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanActionRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanRepository;
import com.arextest.schedule.model.AppServiceDescriptor;
import com.arextest.schedule.model.AppServiceOperationDescriptor;
import com.arextest.schedule.model.CaseSendStatusType;
import com.arextest.schedule.model.CompareProcessStatusType;
import com.arextest.schedule.model.LogType;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.plan.PlanContext;
import com.arextest.schedule.plan.PlanContextCreator;
import com.arextest.schedule.planexecution.PlanExecutionContextProvider;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.utils.ReplayParentBinder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    public int prepareRunData(ReplayPlan replayPlan) {
        metricService.recordTimeEvent(LogType.PLAN_EXECUTION_DELAY.getValue(), replayPlan.getId(), replayPlan.getAppId(), null,
            System.currentTimeMillis() - replayPlan.getPlanCreateMillis());
        int planSavedCaseSize = saveAllActionCase(replayPlan.getReplayActionItemList());
        if (planSavedCaseSize != replayPlan.getCaseTotalCount()) {
            LOGGER.info("update the plan TotalCount, plan id:{} ,appId: {} , size: {} -> {}", replayPlan.getId(),
                replayPlan.getAppId(), replayPlan.getCaseTotalCount(), planSavedCaseSize);
            replayPlan.setCaseTotalCount(planSavedCaseSize);
            replayPlanRepository.updateCaseTotal(replayPlan.getId(), planSavedCaseSize);
            replayReportService.updateReportCaseCount(replayPlan);
        }
        return planSavedCaseSize;
    }

    private int saveAllActionCase(List<ReplayActionItem> replayActionItemList) {
        int planSavedCaseSize = 0;
        long start;
        long end;

        for (ReplayActionItem replayActionItem : replayActionItemList) {
            if (replayActionItem.getReplayStatus() != ReplayStatusType.INIT.getValue()) {
                planSavedCaseSize += replayActionItem.getReplayCaseCount();
                continue;
            }
            int preloaded = replayActionItem.getReplayCaseCount();

            start = System.currentTimeMillis();
            int actionSavedCount = streamingCaseItemSave(replayActionItem);
            end = System.currentTimeMillis();
            BizLogger.recordActionItemCaseSaved(replayActionItem, actionSavedCount, end - start);

            planSavedCaseSize += actionSavedCount;
            if (preloaded != actionSavedCount) {
                replayActionItem.setReplayCaseCount(actionSavedCount);
                LOGGER.warn("The saved case size of actionItem not equals, preloaded size:{},saved size:{}", preloaded,
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
            size = doPagingLoadCaseSave(replayActionItem);
        }
        return size;
    }


    private void caseItemPostProcess(List<ReplayActionCaseItem> caseItemList) {
        // to provide necessary fields into case item for context to consume when sending
        planExecutionContextProvider.injectContextIntoCase(caseItemList);
    }

    /**
     * Paging query storage's recording data.
     * if caseCountLimit > CommonConstant.MAX_PAGE_SIZE, Calculate the latest pageSize and recycle pagination queries
     * <p>
     * else if caseCountLimit < CommonConstant.MAX_PAGE_SIZE or recording data size < request page size,
     * Only need to query once by page
     */
    private int doPagingLoadCaseSave(ReplayActionItem replayActionItem) {
        final ReplayPlan replayPlan = replayActionItem.getParent();
        long beginTimeMills = replayActionItem.getLastRecordTime();
        if (beginTimeMills == 0) {
            beginTimeMills = replayPlan.getCaseSourceFrom().getTime();
        }
        long endTimeMills = replayPlan.getCaseSourceTo().getTime();
        int totalSize = 0;
        int caseCountLimit = replayActionItem.getOperationTypes() == null ? replayPlan.getCaseCountLimit() : replayPlan.getCaseCountLimit() * replayActionItem.getOperationTypes().size();
        int pageSize = Math.min(caseCountLimit, CommonConstant.MAX_PAGE_SIZE);
        while (beginTimeMills < endTimeMills) {
            List<ReplayActionCaseItem> caseItemList = caseRemoteLoadService.pagingLoad(beginTimeMills, endTimeMills,
                replayActionItem, caseCountLimit - totalSize);
            if (CollectionUtils.isEmpty(caseItemList)) {
                break;
            }
            caseItemPostProcess(caseItemList);
            ReplayParentBinder.setupCaseItemParent(caseItemList, replayActionItem);
            totalSize += caseItemList.size();
            beginTimeMills = caseItemList.get(caseItemList.size() - 1).getRecordTime();
            replayActionCaseItemRepository.save(caseItemList);
            if (totalSize >= caseCountLimit || caseItemList.size() < pageSize) {
                break;
            }
            replayActionItem.setLastRecordTime(beginTimeMills);
        }
        return totalSize;
    }

    private int doFixedCaseSave(ReplayActionItem replayActionItem) {
        List<ReplayActionCaseItem> caseItemList = replayActionItem.getCaseItemList();
        int size = 0;
        for (int i = 0; i < caseItemList.size(); i++) {
            ReplayActionCaseItem caseItem = caseItemList.get(i);
            Set<String> operationTypes = caseItem.getParent().getOperationTypes();
            ReplayActionCaseItem viewReplay = caseRemoteLoadService.viewReplayLoad(caseItem, operationTypes);
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
        replayActionCaseItemRepository.save(caseItemList);
        return size;
    }


    public void updateFailedActionAndCase(ReplayPlan replayPlan, List<ReplayActionCaseItem> failedCaseList)
        throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        List<ReplayActionItem> replayActionItems = replayPlanActionRepository.queryPlanActionList(replayPlan.getId());

        replayPlan.setReRunCaseCount(failedCaseList.size());

        Map<String, List<ReplayActionCaseItem>> failedCaseMap = failedCaseList.stream()
            .peek(caseItem -> {
                caseItem.setSendStatus(CaseSendStatusType.WAIT_HANDLING.getValue());
                caseItem.setCompareStatus(CompareProcessStatusType.WAIT_HANDLING.getValue());
            })
            .collect(Collectors.groupingBy(ReplayActionCaseItem::getPlanItemId));

        Map<String, List<String>> actionIdAndRecordIdsMap = failedCaseMap.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream()
                .map(ReplayActionCaseItem::getRecordId).collect(Collectors.toList())));
        executorService.submit(() -> replayReportService.removeRecordsAndScenes(actionIdAndRecordIdsMap));
        executorService.submit(() -> replayActionCaseItemRepository.batchUpdateStatus(failedCaseList));

        List<ReplayActionItem> failedActionList = replayActionItems.stream()
            .filter(actionItem -> CollectionUtils.isNotEmpty(failedCaseMap.get(actionItem.getId())))
            .peek(actionItem -> {
                actionItem.setParent(replayPlan);
                actionItem.setCaseItemList(failedCaseMap.get(actionItem.getId()));
                ReplayParentBinder.setupCaseItemParent(actionItem.getCaseItemList(), actionItem);
            }).collect(Collectors.toList());

        Future future = executorService.submit(
            () -> replayActionItemPreprocessService.filterActionItem(failedActionList, replayPlan.getAppId()));

        future.get();
        executorService.shutdown();
        replayPlan.setReplayActionItemList(failedActionList);
    }

    public void doResumeOperationDescriptor(ReplayPlan replayPlan) {
        PlanContext planContext = planContextCreator.createByAppId(replayPlan.getAppId());
        AppServiceOperationDescriptor operationDescriptor;
        for (ReplayActionItem actionItem : replayPlan.getReplayActionItemList()) {
            operationDescriptor = planContext.findAppServiceOperationDescriptor(actionItem.getOperationId());
            if (operationDescriptor == null) {
                LOGGER.warn("skip resume when the plan operationDescriptor not found, action id: {} ,",
                    actionItem.getId()
                );
                continue;
            }
            AppServiceDescriptor appServiceDescriptor = operationDescriptor.getParent();
            List<ServiceInstance> activeInstanceList = deployedEnvironmentService.getActiveInstanceList(appServiceDescriptor,
                replayPlan.getTargetEnv());
            appServiceDescriptor.setTargetActiveInstanceList(activeInstanceList);

            planContext.fillReplayAction(actionItem, operationDescriptor);
            replayPlan.setMinInstanceCount(planContext.determineMinInstanceCount());
        }
    }
}

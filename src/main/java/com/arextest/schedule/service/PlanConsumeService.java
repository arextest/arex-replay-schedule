package com.arextest.schedule.service;

import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.common.SendSemaphoreLimiter;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.dao.mongodb.ReplayBizLogRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanRepository;
import com.arextest.schedule.mdc.AbstractTracedRunnable;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.*;
import com.arextest.schedule.model.bizlog.BizLog;
import com.arextest.schedule.planexecution.PlanExecutionContextProvider;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.utils.ReplayParentBinder;
import io.netty.util.internal.MathUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * @author jmo
 * @since 2021/9/15
 */
@Slf4j
@Service
@SuppressWarnings("rawtypes")
public final class PlanConsumeService {
    @Resource
    private ReplayCaseRemoteLoadService caseRemoteLoadService;
    @Resource
    private ReplayActionCaseItemRepository replayActionCaseItemRepository;
    @Resource
    private ReplayBizLogRepository replayBizLogRepository;
    @Resource
    private ReplayCaseTransmitService replayCaseTransmitService;
    @Resource
    private ExecutorService preloadExecutorService;
    @Resource
    private ExecutorService actionItemParallelPool;
    @Resource
    private ReplayPlanRepository replayPlanRepository;
    @Resource
    private ProgressTracer progressTracer;
    @Resource
    private ProgressEvent progressEvent;
    @Resource
    private MetricService metricService;
    @Resource
    private PlanExecutionContextProvider planExecutionContextProvider;

    public void runAsyncConsume(ReplayPlan replayPlan) {
        BizLog.recordPlanAsyncStart(replayPlan);
        // TODO: remove block thread use async to load & send for all
        preloadExecutorService.execute(new ReplayActionLoadingRunnableImpl(replayPlan));
    }

    private final class ReplayActionLoadingRunnableImpl extends AbstractTracedRunnable {
        private final ReplayPlan replayPlan;

        private ReplayActionLoadingRunnableImpl(ReplayPlan replayPlan) {
            this.replayPlan = replayPlan;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void doWithTracedRunning() {
            try {
                int planSavedCaseSize = saveActionCaseToSend(replayPlan);
                BizLog.recordPlanCaseSaved(replayPlan, planSavedCaseSize);

                replayPlan.setExecutionContexts(planExecutionContextProvider.buildContext(replayPlan));
                BizLog.recordContextBuilt(replayPlan);

                if (CollectionUtils.isEmpty(replayPlan.getExecutionContexts())) {
                    LOGGER.error("Invalid context built for plan {}", replayPlan);
                    replayPlan.setErrorMessage("Got empty execution context");
                    progressEvent.onReplayPlanInterrupt(replayPlan, ReplayStatusType.FAIL_INTERRUPTED);
                    return;
                }

                sendAllActionCase(replayPlan);

                // actionItems with empty case item are not able to trigger plan finished hook
                if (planSavedCaseSize == 0) {
                    progressEvent.onReplayPlanFinish(replayPlan);
                }
            } finally {
                replayBizLogRepository.saveAll(replayPlan);
            }
        }
    }

    private int saveActionCaseToSend(ReplayPlan replayPlan) {
        metricService.recordTimeEvent(LogType.PLAN_EXECUTION_DELAY.getValue(), replayPlan.getId(), replayPlan.getAppId(), null,
                System.currentTimeMillis() - replayPlan.getPlanCreateMillis());
        int planSavedCaseSize = saveAllActionCase(replayPlan.getReplayActionItemList());
        if (planSavedCaseSize != replayPlan.getCaseTotalCount()) {
            LOGGER.info("update the plan TotalCount, plan id:{} ,appId: {} , size: {} -> {}", replayPlan.getId(),
                    replayPlan.getAppId(), replayPlan.getCaseTotalCount(), planSavedCaseSize);
            replayPlan.setCaseTotalCount(planSavedCaseSize);
            replayPlanRepository.updateCaseTotal(replayPlan.getId(), planSavedCaseSize);
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
            int actionSavedCount = streamingCaseItemSave(replayActionItem);
            replayActionItem.setReplayCaseCount(actionSavedCount);
            planSavedCaseSize += actionSavedCount;
            int preloaded = replayActionItem.getReplayCaseCount();
            if (preloaded != actionSavedCount) {
                replayActionItem.setReplayCaseCount(actionSavedCount);
                LOGGER.warn("The saved case size of actionItem not equals, preloaded size:{},saved size:{}", preloaded,
                        actionSavedCount);
            }
            progressEvent.onActionCaseLoaded(replayActionItem);
        }
        return planSavedCaseSize;
    }

    @SuppressWarnings("unchecked")
    private void sendAllActionCase(ReplayPlan replayPlan) {
        long start, end;
        progressTracer.initTotal(replayPlan);

        // limiter shared for entire plan, max qps = maxQps per instance * min instance count
        final SendSemaphoreLimiter qpsLimiter = new SendSemaphoreLimiter(replayPlan.getReplaySendMaxQps(),
                replayPlan.getMinInstanceCount());
        qpsLimiter.setReplayPlan(replayPlan);
        qpsLimiter.setTotalTasks(replayPlan.getCaseTotalCount());

        BizLog.recordQpsInit(replayPlan, qpsLimiter.getPermits(), replayPlan.getMinInstanceCount());

        ExecutionStatus sendResult = ExecutionStatus.buildNormal();
        for (PlanExecutionContext executionContext : replayPlan.getExecutionContexts()) {
            executionContext.setExecutionStatus(sendResult);
            executionContext.setPlan(replayPlan);

            start = System.currentTimeMillis();
            planExecutionContextProvider.onBeforeContextExecution(executionContext, replayPlan);
            end = System.currentTimeMillis();
            BizLog.recordContextBeforeRun(executionContext, end - start);

            executeContext(replayPlan, qpsLimiter, executionContext);

            start = System.currentTimeMillis();
            planExecutionContextProvider.onAfterContextExecution(executionContext, replayPlan);
            end = System.currentTimeMillis();
            BizLog.recordContextAfterRun(executionContext, end - start);
        }

        if (sendResult.isCanceled()) {
            progressEvent.onReplayPlanFinish(replayPlan, ReplayStatusType.CANCELLED);
            BizLog.recordPlanStatusChange(replayPlan, ReplayStatusType.CANCELLED.name(), "Plan Canceled");
            return;
        }

        if (sendResult.isInterrupted()) {
            progressEvent.onReplayPlanInterrupt(replayPlan, ReplayStatusType.FAIL_INTERRUPTED);
            BizLog.recordPlanStatusChange(replayPlan, ReplayStatusType.FAIL_INTERRUPTED.name(),
                    "Qps limiter with total error count of: " + qpsLimiter.totalError()
                            + " and continuous error of: " + qpsLimiter.continuousError());
            return;
        }
        LOGGER.info("All the plan action sent,waiting to compare, plan id:{} ,appId: {} ", replayPlan.getId(),
                replayPlan.getAppId());
    }

    private void executeContext(ReplayPlan replayPlan, SendSemaphoreLimiter qpsLimiter, PlanExecutionContext executionContext) {
        List<CompletableFuture<Void>> contextTasks = new ArrayList<>();
        for (ReplayActionItem replayActionItem : replayPlan.getReplayActionItemList()) {
            if (!replayActionItem.isItemProcessed()) {
                replayActionItem.setItemProcessed(true);
                MDCTracer.addActionId(replayActionItem.getId());
                replayActionItem.setSendRateLimiter(qpsLimiter);
                if (replayActionItem.isEmpty()) {
                    replayActionItem.setReplayFinishTime(new Date());
                    progressEvent.onActionComparisonFinish(replayActionItem);
                    BizLog.recordActionStatusChange(replayActionItem, ReplayStatusType.FINISHED.name(),
                            "No case needs to be sent");
                }
            }

            if (replayActionItem.finished() || replayActionItem.isEmpty()) {
                LOGGER.warn("Skipped action item: {}, finished: {}, empty: {}",
                        replayActionItem.getAppId(), replayActionItem.finished(), replayActionItem.isEmpty());
                continue;
            }
            CompletableFuture<Void> task = CompletableFuture.runAsync(
                    () -> sendItemByContext(replayActionItem, executionContext),
                    actionItemParallelPool);
            contextTasks.add(task);
        }

        CompletableFuture.allOf(contextTasks.toArray(new CompletableFuture[0])).join();
    }

    private void sendItemByContext(ReplayActionItem replayActionItem, PlanExecutionContext currentContext) {
        BizLog.recordActionUnderContext(replayActionItem, currentContext);

        ExecutionStatus sendResult = currentContext.getExecutionStatus();
        if (sendResult.isCanceled() && replayActionItem.getReplayStatus() != ReplayStatusType.CANCELLED.getValue()) {
            BizLog.recordActionStatusChange(replayActionItem, ReplayStatusType.CANCELLED.name(), "");
            progressEvent.onActionCancelled(replayActionItem);
            return;
        }

        if (sendResult.isInterrupted() && replayActionItem.getReplayStatus() != ReplayStatusType.FAIL_INTERRUPTED.getValue()) {
            progressEvent.onActionInterrupted(replayActionItem);
            BizLog.recordActionInterrupted(replayActionItem);
            return;
        }

        if (replayActionItem.getReplayFinishTime() == null
                && replayActionItem.getReplayStatus() != ReplayStatusType.RUNNING.getValue()) {
            progressEvent.onActionBeforeSend(replayActionItem);
        }

        this.sendByPaging(replayActionItem, currentContext);

        if (sendResult.isInterrupted() && replayActionItem.getReplayStatus() != ReplayStatusType.FAIL_INTERRUPTED.getValue()) {
            progressEvent.onActionInterrupted(replayActionItem);
            BizLog.recordActionInterrupted(replayActionItem);
        }

        if (sendResult.isNormal() &&
                MathUtil.compare(replayActionItem.getReplayCaseCount(), replayActionItem.getCaseProcessCount()) == 0) {
            progressEvent.onActionAfterSend(replayActionItem);
            BizLog.recordActionItemSent(replayActionItem);
        }
    }

    private void sendByPaging(ReplayActionItem replayActionItem, PlanExecutionContext executionContext) {
        ExecutionStatus sendResult = executionContext.getExecutionStatus();
        switch (executionContext.getActionType()) {
            case SKIP_CASE_OF_CONTEXT:
                // skip all cases of this context leaving the status as default
                sendResult.setCanceled(replayCaseTransmitService.releaseCasesOfContext(replayActionItem, executionContext));
                sendResult.setInterrupted(replayActionItem.getSendRateLimiter().failBreak());
                break;

            case NORMAL:
            default:
                List<ReplayActionCaseItem> sourceItemList;
                while (true) {
                    sourceItemList = replayActionCaseItemRepository.waitingSendList(replayActionItem.getId(),
                            CommonConstant.MAX_PAGE_SIZE, executionContext.getContextCaseQuery());

                    replayActionItem.setCaseItemList(sourceItemList);
                    if (CollectionUtils.isEmpty(sourceItemList)) {
                        break;
                    }
                    ReplayParentBinder.setupCaseItemParent(sourceItemList, replayActionItem);
                    sendResult.setInterrupted(replayActionItem.getSendRateLimiter().failBreak());

                    if (sendResult.isInterrupted() || sendResult.isCanceled()) {
                        break;
                    }
                    sendResult.setCanceled(replayCaseTransmitService.send(replayActionItem));
                }
        }
    }

    private int streamingCaseItemSave(ReplayActionItem replayActionItem) {
        List<ReplayActionCaseItem> caseItemList = replayActionItem.getCaseItemList();
        int size;
        if (CollectionUtils.isNotEmpty(caseItemList)) {
            size = doFixedCaseSave(caseItemList);
        } else {
            size = doPagingLoadCaseSave(replayActionItem);
        }
        return size;
    }

    private int doFixedCaseSave(List<ReplayActionCaseItem> caseItemList) {
        caseItemPostProcess(caseItemList);
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
                viewReplay.setParent(caseItem.getParent());
                caseItemList.set(i, viewReplay);
                size++;
            }
        }
        replayActionCaseItemRepository.save(caseItemList);
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
}
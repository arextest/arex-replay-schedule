package com.arextest.schedule.service;

import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.common.SendSemaphoreLimiter;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanRepository;
import com.arextest.schedule.mdc.AbstractTracedRunnable;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.*;
import com.arextest.schedule.planexecution.PlanExecutionContextProvider;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.utils.ReplayParentBinder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.arextest.schedule.common.CommonConstant.PINNED;

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
            int planSavedCaseSize = saveActionCaseToSend(replayPlan);
            replayPlan.setExecutionContexts(planExecutionContextProvider.buildContext(replayPlan));
            if (CollectionUtils.isEmpty(replayPlan.getExecutionContexts())) {
                LOGGER.error("Invalid context built for plan {}", replayPlan);
                progressEvent.onReplayPlanFinish(replayPlan, ReplayStatusType.FAIL_INTERRUPTED);
                return;
            }

            sendAllActionCase(replayPlan);

            // empty items are not able to trigger plan finished hook
            if (planSavedCaseSize == 0) {
                progressEvent.onReplayPlanFinish(replayPlan);
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
        progressTracer.initTotal(replayPlan);

        // limiter share for entire plan, TODO: limiter for each service instance
        final SendSemaphoreLimiter qpsLimiter = new SendSemaphoreLimiter(replayPlan.getReplaySendMaxQps());
        qpsLimiter.setTotalTasks(replayPlan.getCaseTotalCount());

        AtomicReference<ExecutionStatus> sendResult = new AtomicReference<>(ExecutionStatus.buildNormal());

        for (PlanExecutionContext executionContext : replayPlan.getExecutionContexts()) {
            planExecutionContextProvider.onBeforeContextExecution(executionContext, replayPlan);
            List<CompletableFuture<Void>> contextTasks = new ArrayList<>();
            for (ReplayActionItem replayActionItem : replayPlan.getReplayActionItemList()) {
                if (!replayActionItem.isProcessed()) {
                    replayActionItem.setProcessed(true);
                    MDCTracer.addActionId(replayActionItem.getId());
                    replayActionItem.setSendRateLimiter(qpsLimiter);
                    if (replayActionItem.isEmpty()) {
                        replayActionItem.setReplayFinishTime(new Date());
                        progressEvent.onActionComparisonFinish(replayActionItem);
                    }
                }

                if (replayActionItem.finished() || replayActionItem.isEmpty()) {
                    continue;
                }
                CompletableFuture<Void> task = CompletableFuture.runAsync(
                        () -> sendItemByContext(replayActionItem, executionContext, sendResult),
                        actionItemParallelPool);
                contextTasks.add(task);
            }

            CompletableFuture.allOf(contextTasks.toArray(new CompletableFuture[0])).join();
            planExecutionContextProvider.onAfterContextExecution(executionContext, replayPlan);
        }

        if (sendResult.get().isCanceled()) {
            progressEvent.onReplayPlanFinish(replayPlan, ReplayStatusType.CANCELLED);
            LOGGER.info("The plan was isCancelled, plan id:{} ,appId: {} ", replayPlan.getId(),
                    replayPlan.getAppId());
            return;
        }

        if (sendResult.get().isInterrupted()) {
            progressEvent.onReplayPlanInterrupt(replayPlan, ReplayStatusType.FAIL_INTERRUPTED);
            LOGGER.info("The plan was interrupted, plan id:{} ,appId: {} ", replayPlan.getId(),
                    replayPlan.getAppId());
            return;
        }
        LOGGER.info("All the plan action sent,waiting to compare, plan id:{} ,appId: {} ", replayPlan.getId(),
                replayPlan.getAppId());
    }

    private void sendItemByContext(ReplayActionItem replayActionItem, PlanExecutionContext currentContext,
                                              AtomicReference<ExecutionStatus> sendResult) {
        if (sendResult.get().isCanceled() && replayActionItem.getReplayStatus() != ReplayStatusType.CANCELLED.getValue()) {
            progressEvent.onActionCancelled(replayActionItem);
            return;
        }

        if (sendResult.get().isInterrupted() && replayActionItem.getReplayStatus() != ReplayStatusType.FAIL_INTERRUPTED.getValue()) {
            progressEvent.onActionInterrupted(replayActionItem);
            return;
        }

        if (replayActionItem.getReplayFinishTime() == null
                && replayActionItem.getReplayStatus() != ReplayStatusType.RUNNING.getValue()) {
            progressEvent.onActionBeforeSend(replayActionItem);
        }

        ExecutionStatus curStatus = this.sendByPaging(replayActionItem, currentContext);
        if (curStatus.isInterrupted()) {
            progressEvent.onActionInterrupted(replayActionItem);
        }

        if (curStatus.isNormal()) {
            progressEvent.onActionAfterSend(replayActionItem);
        }

        sendResult.set(curStatus);
    }

    private ExecutionStatus sendByPaging(ReplayActionItem replayActionItem, PlanExecutionContext executionContext) {
        List<ReplayActionCaseItem> sourceItemList;
        while (true) {
            sourceItemList = replayActionCaseItemRepository.waitingSendList(replayActionItem.getId(),
                    CommonConstant.MAX_PAGE_SIZE, executionContext.getContextCaseQuery());

            replayActionItem.setCaseItemList(sourceItemList);
            if (CollectionUtils.isEmpty(sourceItemList)) {
                break;
            }
            ReplayParentBinder.setupCaseItemParent(sourceItemList, replayActionItem);

            boolean isCanceled = replayCaseTransmitService.send(replayActionItem);
            boolean isInterrupted = replayActionItem.getSendRateLimiter().failBreak();

            if (isCanceled || isInterrupted) {
                return ExecutionStatus.builder().canceled(isCanceled).interrupted(isInterrupted).build();
            }
        }
        return ExecutionStatus.buildNormal();
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
        int size = 0;
        String sourceProvider = PINNED;
        for (int i = 0; i < caseItemList.size(); i++) {
            ReplayActionCaseItem caseItem = caseItemList.get(i);
            ReplayActionCaseItem viewReplay = caseRemoteLoadService.viewReplayLoad(caseItem, sourceProvider);
            if (viewReplay == null) {
                caseItem.setSendStatus(CaseSendStatusType.REPLAY_CASE_NOT_FOUND.getValue());
            } else {
                viewReplay.setParent(caseItem.getParent());
                caseItemList.set(i, viewReplay);
                size++;
            }
        }
        replayActionCaseItemRepository.save(caseItemList);
        return size;
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
        int caseCountLimit = replayPlan.getCaseCountLimit();
        int pageSize = Math.min(caseCountLimit, CommonConstant.MAX_PAGE_SIZE);
        while (beginTimeMills < endTimeMills) {
            List<ReplayActionCaseItem> caseItemList = caseRemoteLoadService.pagingLoad(beginTimeMills, endTimeMills,
                    replayActionItem, caseCountLimit - totalSize);
            if (CollectionUtils.isEmpty(caseItemList)) {
                break;
            }
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
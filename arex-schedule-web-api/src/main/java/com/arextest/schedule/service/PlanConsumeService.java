package com.arextest.schedule.service;

import com.arextest.schedule.bizlog.BizLogger;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.common.SendSemaphoreLimiter;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.mdc.AbstractTracedRunnable;
import com.arextest.schedule.model.*;
import com.arextest.schedule.planexecution.PlanExecutionContextProvider;
import com.arextest.schedule.planexecution.PlanExecutionMonitor;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.utils.ReplayParentBinder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * @author jmo
 * @since 2021/9/15
 */
@Slf4j
@Service
@SuppressWarnings({"rawtypes", "unchecked"})
public final class PlanConsumeService {
    @Resource
    private PlanConsumePrepareService planConsumePrepareService;
    @Resource
    private ReplayActionCaseItemRepository replayActionCaseItemRepository;
    @Resource
    private ReplayCaseTransmitService replayCaseTransmitService;
    @Resource
    private ExecutorService preloadExecutorService;
    @Resource
    private ProgressTracer progressTracer;
    @Resource
    private ProgressEvent progressEvent;
    @Resource
    private PlanExecutionContextProvider planExecutionContextProvider;
    @Resource
    private PlanExecutionMonitor planExecutionMonitor;

    public void runAsyncConsume(ReplayPlan replayPlan) {
        BizLogger.recordPlanAsyncStart(replayPlan);
        // TODO: remove block thread use async to load & send for all
        preloadExecutorService.execute(new ReplayActionLoadingRunnableImpl(replayPlan));
    }

    private final class ReplayActionLoadingRunnableImpl extends AbstractTracedRunnable {
        private final ReplayPlan replayPlan;

        private ReplayActionLoadingRunnableImpl(ReplayPlan replayPlan) {
            this.replayPlan = replayPlan;
        }

        private void init() {
            // limiter shared for entire plan, max qps = maxQps per instance * min instance count
            final SendSemaphoreLimiter qpsLimiter = new SendSemaphoreLimiter(replayPlan.getReplaySendMaxQps(),
                    replayPlan.getMinInstanceCount());
            qpsLimiter.setTotalTasks(replayPlan.getCaseTotalCount());
            qpsLimiter.setReplayPlan(replayPlan);
            replayPlan.setPlanStatus(ExecutionStatus.buildNormal(qpsLimiter));
            replayPlan.setLimiter(qpsLimiter);
            replayPlan.getReplayActionItemList().forEach(replayActionItem -> replayActionItem.setSendRateLimiter(qpsLimiter));
            replayPlan.buildActionItemMap();
            planExecutionMonitor.register(replayPlan);
            BizLogger.recordQpsInit(replayPlan, qpsLimiter.getPermits(), replayPlan.getMinInstanceCount());
        }
        @Override
        protected void doWithTracedRunning() {
            try {
                long start;
                long end;

                // init limiter, monitor
                this.init();

                // prepare cases to send
                start = System.currentTimeMillis();
                int planSavedCaseSize = planConsumePrepareService.prepareRunData(replayPlan);
                end = System.currentTimeMillis();
                BizLogger.recordPlanCaseSaved(replayPlan, planSavedCaseSize, end - start);

                // build context to send
                start = System.currentTimeMillis();
                replayPlan.setExecutionContexts(planExecutionContextProvider.buildContext(replayPlan));
                end = System.currentTimeMillis();
                BizLogger.recordContextBuilt(replayPlan, end - start);

                if (CollectionUtils.isEmpty(replayPlan.getExecutionContexts())) {
                    LOGGER.error("Invalid context built for plan {}", replayPlan);
                    replayPlan.setErrorMessage("Got empty execution context");
                    progressEvent.onReplayPlanInterrupt(replayPlan, ReplayStatusType.FAIL_INTERRUPTED);
                    BizLogger.recordPlanStatusChange(replayPlan, ReplayStatusType.FAIL_INTERRUPTED.name(),
                            "NO context to execute");
                    return;
                }

                // process plan
                consumePlan(replayPlan);

                // finalize exceptional status
                finalizePlanStatus(replayPlan);

            } catch (Throwable t) {
                BizLogger.recordPlanException(replayPlan, t);
                throw t;
            } finally {
                planExecutionMonitor.deregister(replayPlan);
            }
        }
    }

    private void consumePlan(ReplayPlan replayPlan) {
        ExecutionStatus executionStatus = replayPlan.getPlanStatus();

        long start;
        long end;
        progressTracer.initTotal(replayPlan);

        for (PlanExecutionContext executionContext : replayPlan.getExecutionContexts()) {
            // checkpoint: before each context
            if (executionStatus.isAbnormal()) {
                break;
            }

            executionContext.setExecutionStatus(executionStatus);
            executionContext.setPlan(replayPlan);

            // before context hook, may contain job like instructing target instance to prepare environment
            start = System.currentTimeMillis();
            planExecutionContextProvider.onBeforeContextExecution(executionContext, replayPlan);
            end = System.currentTimeMillis();
            BizLogger.recordContextBeforeRun(executionContext, end - start);

            consumeContext(replayPlan, executionContext);

            start = System.currentTimeMillis();
            planExecutionContextProvider.onAfterContextExecution(executionContext, replayPlan);
            end = System.currentTimeMillis();
            BizLogger.recordContextAfterRun(executionContext, end - start);
        }
    }

    private void consumeContext(ReplayPlan replayPlan, PlanExecutionContext executionContext) {
        if (executionContext.getExecutionStatus().isAbnormal()) {
            return;
        }
        switch (executionContext.getActionType()) {
            case SKIP_CASE_OF_CONTEXT:
                // skip all cases of this context leaving the status as default
                replayCaseTransmitService.releaseCasesOfContext(replayPlan, executionContext);
                break;
            case NORMAL:
            default:
                this.consumeContextPaged(replayPlan, executionContext);
        }
    }


    private void consumeContextPaged(ReplayPlan replayPlan, PlanExecutionContext executionContext) {
        ExecutionStatus executionStatus = executionContext.getExecutionStatus();

        int contextCount = 0;
        List<ReplayActionCaseItem> caseItems = Collections.emptyList();
        while (true) {
            // checkpoint: before sending page of cases
            if (executionStatus.isAbnormal()) {
                break;
            }
            ReplayActionCaseItem lastItem = caseItems.size() > 0 ? caseItems.get(caseItems.size() - 1) : null;
            caseItems = replayActionCaseItemRepository.waitingSendList(replayPlan.getId(),
                    CommonConstant.MAX_PAGE_SIZE,
                    executionContext.getContextCaseQuery(),
                    Optional.ofNullable(lastItem).map(ReplayActionCaseItem::getId).orElse(null));

            if (CollectionUtils.isEmpty(caseItems)) {
                break;
            }
            contextCount += caseItems.size();
            ReplayParentBinder.setupCaseItemParent(caseItems, replayPlan);
            replayCaseTransmitService.send(caseItems, executionContext);
        }
        BizLogger.recordContextProcessedNormal(executionContext, contextCount);
    }

    private void finalizePlanStatus(ReplayPlan replayPlan) {
        ExecutionStatus executionStatus = replayPlan.getPlanStatus();

        planExecutionMonitor.monitorOne(replayPlan);

        // finalize plan status
        if (executionStatus.isCanceled()) {
            progressEvent.onReplayPlanFinish(replayPlan, ReplayStatusType.CANCELLED);
            BizLogger.recordPlanStatusChange(replayPlan, ReplayStatusType.CANCELLED.name(), "Plan Canceled");
        } else if (executionStatus.isInterrupted()) {
            progressEvent.onReplayPlanInterrupt(replayPlan, ReplayStatusType.FAIL_INTERRUPTED);
            BizLogger.recordPlanStatusChange(replayPlan, ReplayStatusType.FAIL_INTERRUPTED.name(), "Plan Interrupted by QPS limiter.");
        } else if (replayPlan.getCaseTotalCount() == 0) {
            progressEvent.onReplayPlanFinish(replayPlan);
            BizLogger.recordPlanStatusChange(replayPlan, ReplayStatusType.FINISHED.name(), "No cases to send.");
        } else {
            BizLogger.recordPlanDone(replayPlan);
            LOGGER.info("All the plan action sent,waiting to compare, plan id:{} ,appId: {} ", replayPlan.getId(), replayPlan.getAppId());
        }

        for (ReplayActionItem replayActionItem : replayPlan.getReplayActionItemList()) {
            if (executionStatus.isCanceled() && !replayActionItem.sendDone()) {
                progressEvent.onActionCancelled(replayActionItem);
                BizLogger.recordActionStatusChange(replayActionItem, ReplayStatusType.CANCELLED.name(), "");
            } else if (executionStatus.isInterrupted() && !replayActionItem.sendDone()) {
                progressEvent.onActionInterrupted(replayActionItem);
                BizLogger.recordActionStatusChange(replayActionItem, ReplayStatusType.FAIL_INTERRUPTED.name(), "");
            }
        }
    }
}
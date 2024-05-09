package com.arextest.schedule.service;

import com.arextest.schedule.bizlog.BizLogger;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.common.SendSemaphoreLimiter;
import com.arextest.schedule.comparer.CompareConfigService;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.mdc.AbstractTracedRunnable;
import com.arextest.schedule.model.ExecutionStatus;
import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.model.plan.PlanStageEnum;
import com.arextest.schedule.model.plan.StageStatusEnum;
import com.arextest.schedule.planexecution.PlanExecutionContextProvider;
import com.arextest.schedule.planexecution.PlanExecutionMonitor;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.utils.ReplayParentBinder;
import com.arextest.schedule.utils.StageUtils;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

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
  private ReplayCaseTransmitServiceImpl replayCaseTransmitServiceRemoteImpl;
  @Resource
  private ExecutorService preloadExecutorService;
  @Resource
  private ProgressTracer progressTracer;
  @Resource
  private ProgressEvent progressEvent;
  @Resource
  private PlanExecutionContextProvider planExecutionContextProvider;
  @Resource
  private PlanExecutionMonitor planExecutionMonitorImpl;
  @Resource
  private CompareConfigService compareConfigService;

  public void runAsyncConsume(ReplayPlan replayPlan) {
    BizLogger.recordPlanAsyncStart(replayPlan);
    // TODO: remove block thread use async to load & send for all
    preloadExecutorService.execute(new ReplayActionLoadingRunnableImpl(replayPlan));
  }

  private void consumePlan(ReplayPlan replayPlan) {
    ExecutionStatus executionStatus = replayPlan.getPlanStatus();

    long start;
    long end;
    progressTracer.initTotal(replayPlan);
    if (replayPlan.isReRun()) {
      // correct counter
      progressTracer.reRunPlan(replayPlan);
    }
    int index = 0, total = replayPlan.getExecutionContexts().size();
    for (PlanExecutionContext executionContext : replayPlan.getExecutionContexts()) {
      index++;
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
      LOGGER.info("context {} start hook took {} ms", executionContext.getContextName(),
          end - start);

      consumeContext(replayPlan, executionContext);

      start = System.currentTimeMillis();
      planExecutionContextProvider.onAfterContextExecution(executionContext, replayPlan);
      end = System.currentTimeMillis();
      LOGGER.info("context {} finish hook took {} ms", executionContext.getContextName(),
          end - start);

      StageStatusEnum stageStatusEnum = null;
      Long endTime = null;
      String format = StageUtils.RUN_MSG_FORMAT;
      if (index == total) {
        stageStatusEnum = StageStatusEnum.SUCCEEDED;
        endTime = System.currentTimeMillis();
      }
      if (index == 1) {
        format = StageUtils.RUN_MSG_FORMAT_SINGLE;
      }
      PlanStageEnum planStageEnum = replayPlan.isReRun() ? PlanStageEnum.RE_RUN : PlanStageEnum.RUN;
      progressEvent.onReplayPlanStageUpdate(replayPlan, planStageEnum, stageStatusEnum,
          null, endTime, String.format(format, total, index));
    }
  }

  private void consumeContext(ReplayPlan replayPlan, PlanExecutionContext executionContext) {
    if (executionContext.getExecutionStatus().isAbnormal()) {
      return;
    }
    switch (executionContext.getActionType()) {
      case SKIP_CASE_OF_CONTEXT:
        // skip all cases of this context leaving the status as default
        replayCaseTransmitServiceRemoteImpl.releaseCasesOfContext(replayPlan, executionContext);
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
      ReplayActionCaseItem lastItem =
          CollectionUtils.isNotEmpty(caseItems) ? caseItems.get(caseItems.size() - 1) : null;
      caseItems = replayActionCaseItemRepository.waitingSendList(replayPlan.getId(),
          CommonConstant.MAX_PAGE_SIZE,
          executionContext.getContextCaseQuery(),
          Optional.ofNullable(lastItem).map(ReplayActionCaseItem::getId).orElse(null));

      if (CollectionUtils.isEmpty(caseItems)) {
        break;
      }
      contextCount += caseItems.size();
      ReplayParentBinder.setupCaseItemParent(caseItems, replayPlan);
      replayCaseTransmitServiceRemoteImpl.send(caseItems, executionContext);
    }
  }

  private void finalizePlanStatus(ReplayPlan replayPlan) {
    progressEvent.onReplayPlanStageUpdate(replayPlan, PlanStageEnum.FINISH, StageStatusEnum.ONGOING,
        System.currentTimeMillis(), null);

    ExecutionStatus executionStatus = replayPlan.getPlanStatus();

    // finalize plan status
    if (executionStatus.isCanceled()) {
      progressEvent.onReplayPlanFinish(replayPlan, ReplayStatusType.CANCELLED);
      BizLogger.recordPlanStatusChange(replayPlan, ReplayStatusType.CANCELLED);
    } else if (executionStatus.isInterrupted()) {
      progressEvent.onReplayPlanInterrupt(replayPlan, ReplayStatusType.FAIL_INTERRUPTED);
    } else if (replayPlan.getCaseTotalCount() == 0) {
      progressEvent.onReplayPlanFinish(replayPlan);
    } else {
      BizLogger.recordPlanDone(replayPlan);
    }

    progressEvent.onReplayPlanStageUpdate(replayPlan, PlanStageEnum.FINISH,
        StageStatusEnum.SUCCEEDED, null, System.currentTimeMillis());
  }

  private final class ReplayActionLoadingRunnableImpl extends AbstractTracedRunnable {

    private final ReplayPlan replayPlan;

    private ReplayActionLoadingRunnableImpl(ReplayPlan replayPlan) {
      this.replayPlan = replayPlan;
    }

    @Override
    protected void doWithTracedRunning() {
      try {
        if (!initCaseCount()) {
          return;
        }
        // init compareConfig,limiter, monitor
        initReplayPlan();

        // build context to send
        progressEvent.onReplayPlanStageUpdate(replayPlan, PlanStageEnum.BUILD_CONTEXT,
            StageStatusEnum.ONGOING,
            System.currentTimeMillis(), null);
        replayPlan.setExecutionContexts(planExecutionContextProvider.buildContext(replayPlan));

        if (CollectionUtils.isEmpty(replayPlan.getExecutionContexts())) {
          LOGGER.error("Invalid context built for plan {}", replayPlan);
          replayPlan.setErrorMessage("Got empty execution context");
          progressEvent.onReplayPlanInterrupt(replayPlan, ReplayStatusType.FAIL_INTERRUPTED);
          progressEvent.onReplayPlanStageUpdate(replayPlan, PlanStageEnum.BUILD_CONTEXT,
              StageStatusEnum.FAILED, null, System.currentTimeMillis());
          return;
        }
        progressEvent.onReplayPlanStageUpdate(replayPlan, PlanStageEnum.BUILD_CONTEXT,
            StageStatusEnum.SUCCEEDED, null, System.currentTimeMillis());

        // process plan
        PlanStageEnum planStageEnum =
            replayPlan.isReRun() ? PlanStageEnum.RE_RUN : PlanStageEnum.RUN;
        progressEvent.onReplayPlanStageUpdate(replayPlan, planStageEnum, StageStatusEnum.ONGOING,
            System.currentTimeMillis(), null);
        consumePlan(replayPlan);

        // finalize exceptional status
        finalizePlanStatus(replayPlan);

      } catch (Throwable t) {
        BizLogger.recordPlanException(replayPlan, t);
        throw t;
      } finally {
        planExecutionMonitorImpl.deregister(replayPlan);
      }
    }

    private boolean initCaseCount() {
      long start = System.currentTimeMillis();
      progressEvent.onReplayPlanStageUpdate(replayPlan, PlanStageEnum.LOADING_CASE,
          StageStatusEnum.ONGOING, start, null);
      int planSavedCaseSize = planConsumePrepareService.preparePlan(replayPlan);
      long end = System.currentTimeMillis();
      progressEvent.onReplayPlanStageUpdate(replayPlan, PlanStageEnum.LOADING_CASE,
          StageStatusEnum.SUCCEEDED, null, end);
      if (planSavedCaseSize == 0) {
        LOGGER.warn("No case found, please change the time range and try again. {}", replayPlan.getId());
        String message = "No case found, please change the time range and try again.";
        progressEvent.onReplayPlanTerminate(replayPlan.getId(), message);
        BizLogger.recordPlanStatusChange(replayPlan, ReplayStatusType.CANCELLED, message);
        return false;
      }
      return true;
    }

    private void initReplayPlan() {
      compareConfigService.preload(replayPlan);

      // limiter shared for entire plan, max qps = maxQps per instance * min instance count
      final SendSemaphoreLimiter qpsLimiter = new SendSemaphoreLimiter(
          replayPlan.getReplaySendMaxQps(),
          replayPlan.getMinInstanceCount());
      qpsLimiter.setTotalTasks(replayPlan.getCaseTotalCount());
      qpsLimiter.setReplayPlan(replayPlan);
      replayPlan.setPlanStatus(ExecutionStatus.buildNormal(qpsLimiter));
      replayPlan.setLimiter(qpsLimiter);
      replayPlan.getReplayActionItemList()
          .forEach(replayActionItem -> replayActionItem.setSendRateLimiter(qpsLimiter));
      replayPlan.buildActionItemMap();

      LOGGER.info("plan {} init with rate {}", replayPlan, qpsLimiter.getPermits());
    }
  }
}
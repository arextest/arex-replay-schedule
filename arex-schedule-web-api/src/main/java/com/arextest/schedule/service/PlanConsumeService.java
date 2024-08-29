package com.arextest.schedule.service;

import com.arextest.model.replay.CaseSendScene;
import com.arextest.schedule.bizlog.BizLogger;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.common.RateLimiterFactory;
import com.arextest.schedule.common.SendSemaphoreLimiter;
import com.arextest.schedule.comparer.CompareConfigService;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.mdc.AbstractTracedRunnable;
import com.arextest.schedule.model.ExecutionStatus;
import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.model.plan.BuildReplayPlanType;
import com.arextest.schedule.model.plan.PlanStageEnum;
import com.arextest.schedule.model.plan.StageStatusEnum;
import com.arextest.schedule.planexecution.PlanExecutionContextProvider;
import com.arextest.schedule.planexecution.PlanExecutionMonitor;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.utils.ReplayParentBinder;
import com.arextest.schedule.utils.ServiceUrlUtils;
import com.arextest.schedule.utils.StageUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${arex.replay.threshold.errorBreakRatio:0.1}")
  private double errorBreakRatio;
  @Value("${arex.replay.threshold.continuousFail:40}")
  private int continuousFailThreshold;

  public void runAsyncConsume(ReplayPlan replayPlan) {
    BizLogger.recordPlanAsyncStart(replayPlan);
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
      // make it possible to determine the interruption in the end
      if (index == total) {
        doFailBreak(executionContext.getWarmupFailedServerUrls(), replayPlan);
      }

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
    List<ReplayActionCaseItem> caseItems = Collections.emptyList();
    final Set<String> lastBatchIds = new HashSet<>();
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
          Optional.ofNullable(lastItem).map(ReplayActionCaseItem::getRecordTime).orElse(null));

      caseItems.removeIf(caseItem -> lastBatchIds.contains(caseItem.getId()));
      if (CollectionUtils.isEmpty(caseItems)) {
        break;
      }

      lastBatchIds.clear();
      lastBatchIds.addAll(
          caseItems.stream().map(ReplayActionCaseItem::getId).collect(Collectors.toSet()));

      ReplayParentBinder.setupCaseItemParent(caseItems, replayPlan);
      caseItemPostProcess(caseItems);
      setCurrentServiceInstances(executionContext, caseItems);
      replayCaseTransmitServiceRemoteImpl.send(caseItems, executionContext);
    }
  }

  /**
   * set service instances for the current context
   *
   * @param executionContext plan execution context
   * @param caseItems        case items
   */
  private static void setCurrentServiceInstances(PlanExecutionContext executionContext,
      List<ReplayActionCaseItem> caseItems) {
    if (executionContext.getCurrentTargetInstances() != null) {
      return;
    }

    Optional<ReplayActionCaseItem> caseItemOptional = caseItems
        .stream().filter(caseItem -> caseItem.getParent() != null)
        .findFirst();
    if (!caseItemOptional.isPresent()) {
      return;
    }
    ReplayActionItem replayActionItem = caseItemOptional.get().getParent();
    List<ServiceInstance> targetInstances = replayActionItem.getTargetInstance();
    if (targetInstances == null) {
      return;
    }
    List<ServiceInstance> sourceInstances = replayActionItem.getSourceInstance();
    try {
      List<String> warmupFailedServiceUrls = executionContext.getWarmupFailedServerUrls();
      if (CollectionUtils.isEmpty(warmupFailedServiceUrls)) {
        executionContext.setCurrentTargetInstances(targetInstances);
        executionContext.setCurrentSourceInstances(replayActionItem.getSourceInstance());
        return;
      }
      // warmup failed instances should be excluded
      Set<String> warmupFailedHostSet = warmupFailedServiceUrls
          .stream()
          .map(ServiceUrlUtils::getHost)
          .collect(Collectors.toSet());

      // if the instance warmup failed, it should be excluded on the current replay
      targetInstances = targetInstances
          .stream()
          .filter(instance -> !warmupFailedHostSet.contains(instance.getIp()))
          .collect(Collectors.toList());
      executionContext.setCurrentTargetInstances(targetInstances);

      if (CollectionUtils.isNotEmpty(sourceInstances)) {
        sourceInstances = sourceInstances
            .stream()
            .filter(instance -> !warmupFailedHostSet.contains(instance.getIp()))
            .collect(Collectors.toList());
        executionContext.setCurrentSourceInstances(sourceInstances);
      }
    } finally {
      // keep the target instances count >= source instances count for rate limit
      reBalanceServiceInstances(executionContext);
      // bind target and source instance for build rate limiter on target instance
      bindTargetAndSource(executionContext);

      String targetHosts = targetInstances.stream().map(ServiceInstance::getIp)
          .collect(Collectors.joining(","));
      String sourceHosts = CollectionUtils.isNotEmpty(sourceInstances) ? sourceInstances
          .stream()
          .map(ServiceInstance::getIp)
          .collect(Collectors.joining(",")) : null;
      BizLogger.recordCurrentServerInstances(executionContext.getPlan(), targetHosts, sourceHosts);
      LOGGER.info(
          "[[title=setCurrentServiceInstances]] set service instances for the current context," +
              "targetInstances: {}, sourceInstances: {}",
          targetInstances, sourceInstances);
    }
  }

  /**
   * if source instances are less than target instances, re-balance the target instances
   *
   * @param executionContext plan execution context
   */
  private static void reBalanceServiceInstances(PlanExecutionContext executionContext) {
    List<ServiceInstance> currentTargetInstances = executionContext.getCurrentTargetInstances();
    List<ServiceInstance> currentSourceInstances = executionContext.getCurrentSourceInstances();
    if (CollectionUtils.isNotEmpty(currentSourceInstances)
        && (currentSourceInstances.size() < currentTargetInstances.size())) {
      currentTargetInstances.subList(currentSourceInstances.size(), currentTargetInstances.size())
          .clear();
    }
  }

  /**
   * bind target and source instance for build rate limiter on target instances
   *
   * @param planExecutionContext the plan execution context
   */
  private static void bindTargetAndSource(PlanExecutionContext planExecutionContext) {
    // avoid rebind
    if (planExecutionContext.getBindInstanceMap() != null) {
      return;
    }

    List<ServiceInstance> targetInstances = planExecutionContext.getCurrentTargetInstances();
    List<ServiceInstance> sourceInstances = planExecutionContext.getCurrentSourceInstances();

    if (CollectionUtils.isEmpty(targetInstances) || CollectionUtils.isEmpty(sourceInstances)) {
      planExecutionContext.setBindInstanceMap(Collections.emptyMap());
      return;
    }

    Map<ServiceInstance, List<ServiceInstance>> bindInstanceMap = new HashMap<>(
        targetInstances.size());
    planExecutionContext.setBindInstanceMap(bindInstanceMap);

    for (int i = 0; i < sourceInstances.size(); i++) {
      ServiceInstance sourceServiceInstance = sourceInstances.get(i);
      int sourceIndex = i % targetInstances.size();
      ServiceInstance targetServiceInstance = targetInstances.get(sourceIndex);
      bindInstanceMap.computeIfAbsent(targetServiceInstance, k -> new ArrayList<>())
          .add(sourceServiceInstance);
    }
    LOGGER.info("bind target and source instance for build rate limiter on target instances, " +
        "bindInstanceMap: {}", bindInstanceMap);
  }

  private void caseItemPostProcess(List<ReplayActionCaseItem> cases) {
    if (CollectionUtils.isEmpty(cases)) {
      return;
    }

    boolean isMixedMode = Optional.ofNullable(cases.get(0).getParent())
        .map(ReplayActionItem::getParent)
        .map(ReplayPlan::getReplayPlanType)
        .map(planType -> planType == BuildReplayPlanType.MIXED.getValue())
        .orElse(false);

    cases.forEach(caseItem -> {
      caseItem.setCaseSendScene(isMixedMode ? CaseSendScene.MIXED_NORMAL : CaseSendScene.NORMAL);
    });
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

  /**
   * do fail break for the failed server urls on the last context, Make it possible to determine the
   * interruption in the end.
   *
   * @param warmupFailedServerUrls the failed server urls
   * @param replayPlan             the replay plan
   */
  private void doFailBreak(List<String> warmupFailedServerUrls, ReplayPlan replayPlan) {
    if (CollectionUtils.isEmpty(warmupFailedServerUrls)) {
      return;
    }
    int failThreshold = continuousFailThreshold + 1;

    try {
      List<SendSemaphoreLimiter> sendSemaphoreLimiterList = replayPlan
          .getPlanStatus()
          .getSendSemaphoreLimiterList()
          .stream()
          .filter(obj -> warmupFailedServerUrls.contains(obj.getHost()))
          .collect(Collectors.toList());
      sendSemaphoreLimiterList.forEach(obj -> {
        if (!obj.failBreak()) {
          obj.batchRelease(false, failThreshold);
        }
      });
    } catch (Exception exception) {
      LOGGER.warn("doFailBreak failed, warmupFailedServerUrls: {}, replayPlan: {}",
          warmupFailedServerUrls, replayPlan, exception);
    }
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
      if (replayPlan.isReRun()) {
        return true;
      }

      long start = System.currentTimeMillis();
      progressEvent.onReplayPlanStageUpdate(replayPlan, PlanStageEnum.LOADING_CASE,
          StageStatusEnum.ONGOING, start, null);
      int planSavedCaseSize = planConsumePrepareService.preparePlan(replayPlan);
      long end = System.currentTimeMillis();
      progressEvent.onReplayPlanStageUpdate(replayPlan, PlanStageEnum.LOADING_CASE,
          StageStatusEnum.SUCCEEDED, null, end);
      if (planSavedCaseSize == 0) {
        LOGGER.warn("No case found, please change the time range and try again. {}",
            replayPlan.getId());
        String message = "No case found, please change the time range and try again.";
        progressEvent.onReplayPlanTerminate(replayPlan.getId(), message);
        BizLogger.recordPlanStatusChange(replayPlan, ReplayStatusType.CANCELLED, message);
        return false;
      }
      return true;
    }

    private void initReplayPlan() {
      compareConfigService.preload(replayPlan);
      // init rate limiter for each target instance
      Collection<SendSemaphoreLimiter> sendSemaphoreLimiterList = this.initRateLimiters(replayPlan);
      replayPlan.setPlanStatus(ExecutionStatus.buildNormal(sendSemaphoreLimiterList));
      replayPlan.buildActionItemMap();
    }

    /**
     * init the rate limiter factory
     *
     * @param replayPlan the replay plan
     */
    private Collection<SendSemaphoreLimiter> initRateLimiters(ReplayPlan replayPlan) {
      if (replayPlan == null) {
        return Collections.emptyList();
      }
      List<ReplayActionItem> replayActionItems = replayPlan.getReplayActionItemList();
      if (CollectionUtils.isEmpty(replayActionItems)) {
        return Collections.emptyList();
      }
      // distinct target instances
      List<ServiceInstance> distinctTargetInstances = replayActionItems
          .stream()
          .map(ReplayActionItem::getTargetInstance)
          .flatMap(Collection::stream)
          .distinct()
          .collect(Collectors.toList());

      if (CollectionUtils.isEmpty(distinctTargetInstances)) {
        return Collections.emptyList();
      }
      int singleTasks = replayPlan.getCaseTotalCount() / distinctTargetInstances.size();
      RateLimiterFactory rateLimiterFactory = new RateLimiterFactory(singleTasks, errorBreakRatio,
          continuousFailThreshold, replayPlan.getReplaySendMaxQps());
      for (ServiceInstance targetInstance : distinctTargetInstances) {
        rateLimiterFactory.getRateLimiter(targetInstance.getIp());
        LOGGER.info("[[title=RateLimiterFactory]] create sendSemaphoreLimiter,ip [{}]",
            targetInstance.getIp());
      }
      replayPlan.setRateLimiterFactory(rateLimiterFactory);
      LOGGER.info("[[title=RateLimiterFactory]] init success for [{}]", replayPlan.getPlanName());

      return rateLimiterFactory.getAll();
    }
  }
}
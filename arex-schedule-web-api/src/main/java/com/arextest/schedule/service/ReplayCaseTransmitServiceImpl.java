package com.arextest.schedule.service;

import com.arextest.common.config.DefaultApplicationConfig;
import com.arextest.schedule.bizlog.BizLogger;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.common.SendSemaphoreLimiter;
import com.arextest.schedule.comparer.ComparisonWriter;
import com.arextest.schedule.comparer.ReplayResultComparer;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.CaseSendStatusType;
import com.arextest.schedule.model.ExecutionStatus;
import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.sender.ReplaySender;
import com.arextest.schedule.sender.ReplaySenderFactory;
import com.arextest.schedule.service.noise.ReplayNoiseIdentify;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import com.arextest.schedule.utils.ServiceUrlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author jmo
 * @since 2021/9/13
 */
@Service
@Slf4j
public class ReplayCaseTransmitServiceImpl implements ReplayCaseTransmitService {

  private static final String COMPARE_DELAY_SECONDS = "compare.delay.seconds";
  private static final int DEFAULT_COMPARE_DELAY_SECONDS = 60;
  @Resource
  private ExecutorService sendExecutorService;
  @Resource
  private ExecutorService compareExecutorService;
  @Resource
  private ReplayResultComparer replayResultComparer;
  @Resource
  private ScheduledExecutorService compareScheduleExecutorService;
  @Resource
  private ReplayCompareService replayCompareService;
  @Resource
  private ReplayActionCaseItemRepository replayActionCaseItemRepository;
  @Resource
  private ProgressTracer progressTracer;
  @Resource
  private ReplaySenderFactory senderFactory;
  @Resource
  private ComparisonWriter comparisonWriter;
  @Resource
  private ProgressEvent progressEvent;
  @Resource
  private MetricService metricService;
  @Resource
  private ReplayNoiseIdentify replayNoiseIdentify;
  @Resource
  private DefaultApplicationConfig defaultConfig;

  @Resource
  private ExecutorService distributeExecutorService;

  public void send(List<ReplayActionCaseItem> caseItems, PlanExecutionContext<?> executionContext) {
    ExecutionStatus executionStatus = executionContext.getExecutionStatus();

    if (executionStatus.isAbnormal()) {
      return;
    }

    prepareActionItems(caseItems);

    replayNoiseIdentify.noiseIdentify(caseItems, executionContext);

    try {
      doSendValuesToRemoteHost(caseItems, executionContext);
    } catch (Throwable throwable) {
      LOGGER.error("do send error:{}", throwable.getMessage(), throwable);
      markAllSendStatus(caseItems, CaseSendStatusType.EXCEPTION_FAILED);
    }
  }

  private void prepareActionItems(List<ReplayActionCaseItem> caseItems) {
    Map<ReplayActionItem, List<ReplayActionCaseItem>> actionsOfBatch = caseItems.stream()
        .filter(caseItem -> caseItem.getParent() != null)
        .collect(Collectors.groupingBy(ReplayActionCaseItem::getParent));

    actionsOfBatch.forEach((actionItem, casesOfAction) -> {
      // warmUp should be done once for each endpoint
      if (!actionItem.isItemProcessed()) {
        actionItem.setItemProcessed(true);
        progressEvent.onActionBeforeSend(actionItem);
      }
    });
  }

  public void releaseCasesOfContext(ReplayPlan replayPlan,
      PlanExecutionContext<?> executionContext) {
    // checkpoint: before skipping batch of group
    if (executionContext.getExecutionStatus().isAbnormal()) {
      return;
    }

    Map<String, Long> caseCountMap = replayActionCaseItemRepository.countWaitHandlingByAction(
        replayPlan.getId(),
        executionContext.getContextCaseQuery());
    Map<String, ReplayActionItem> actionItemMap = replayPlan.getActionItemMap();

    for (Map.Entry<String, Long> actionIdToCaseCount : caseCountMap.entrySet()) {
      ReplayActionItem replayActionItem = actionItemMap.get(actionIdToCaseCount.getKey());
      int contextCasesCount = actionIdToCaseCount.getValue().intValue();
      replayActionItem.recordProcessCaseCount(contextCasesCount);
      // if we skip the rest of cases remaining in the action item, set its status
      if (replayActionItem.getReplayCaseCount() == replayActionItem.getCaseProcessCount()
          .intValue()) {
        progressTracer.finishCaseByPlan(replayPlan, contextCasesCount);
      } else {
        progressTracer.finishCaseByAction(replayActionItem, contextCasesCount);
      }
      BizLogger.recordContextSkipped(executionContext, replayActionItem, contextCasesCount);
      LOGGER.info("action item {} skip {} cases", replayActionItem.getId(), contextCasesCount);
    }
  }

  private void doSendValuesToRemoteHost(List<ReplayActionCaseItem> values,
      PlanExecutionContext<?> planExecutionContext) {

    List<ServiceInstance> targetServiceInstances = planExecutionContext.getCurrentTargetInstances();
    if (CollectionUtils.isEmpty(targetServiceInstances)) {
      LOGGER.warn("The target instance list is empty,skip send.");
      markAllSendStatus(values, CaseSendStatusType.READY_DEPENDENCY_FAILED);
      return;
    }
    final int valueSize = values.size();
    final CountDownLatch groupSentLatch = new CountDownLatch(valueSize);
    ArrayBlockingQueue<ReplayActionCaseItem> caseItemArrayBlockingQueue = new ArrayBlockingQueue<>(
        valueSize, false, values);

    // calculate the number of distribution tasks
    int distributeTaskNum = Math.min(valueSize, targetServiceInstances.size());
    CompletableFuture<?>[] distributeCompletableFutures = new CompletableFuture<?>[distributeTaskNum];
    for (int i = 0; i < distributeTaskNum; i++) {
      ServiceInstance serviceInstance = targetServiceInstances.get(i);
      distributeCompletableFutures[i] = CompletableFuture.runAsync(()
              -> doDistribute(caseItemArrayBlockingQueue, serviceInstance, groupSentLatch,
              planExecutionContext),
          distributeExecutorService);
    }
    try {
      CompletableFuture
          .allOf(distributeCompletableFutures)
          .exceptionally(throwable -> {
            LOGGER.error("An unknown distribution exception has occurred.{}",
                throwable.getMessage(), throwable);
            return null;
          })
          .get(CommonConstant.DISTRIBUTE_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (TimeoutException | InterruptedException | ExecutionException exception) {
      LOGGER.error("do distribute exception:{}", exception.getMessage(), exception);
      Thread.currentThread().interrupt();
    }

    if (planExecutionContext.getExecutionStatus().isAbnormal()) {
      LOGGER.warn("The execution status is abnormal.");
      return;
    }
    // if existed not send cases, mark them as failed
    if (!caseItemArrayBlockingQueue.isEmpty()) {
      List<ReplayActionCaseItem> notSendCases = new ArrayList<>(caseItemArrayBlockingQueue);
      markAllSendStatus(notSendCases, CaseSendStatusType.EXCEPTION_FAILED);
      notSendCases.forEach(caseItem -> groupSentLatch.countDown());
      LOGGER.warn("exist not send cases, mark them as failed, case size:{}", notSendCases.size());
    }

    try {
      boolean clear = groupSentLatch.await(CommonConstant.GROUP_SENT_WAIT_TIMEOUT_SECONDS,
          TimeUnit.SECONDS);
      if (!clear) {
        LOGGER.error("Send group failed to await all request of batch");
      }
    } catch (InterruptedException e) {
      LOGGER.error("send group to remote host error:{}", e.getMessage(), e);
      Thread.currentThread().interrupt();
    }
    MDCTracer.removeDetailId();
  }

  private void doDistribute(ArrayBlockingQueue<ReplayActionCaseItem> caseItemArrayBlockingQueue,
      ServiceInstance targetServiceInstance,
      CountDownLatch groupSentLatch,
      PlanExecutionContext<?> planExecutionContext) {

    SendSemaphoreLimiter currentLimiter = planExecutionContext
        .getPlan()
        .getRateLimiterFactory()
        .getRateLimiter(targetServiceInstance.getIp());
    if (currentLimiter == null) {
      LOGGER.warn("The current service instance - [{}],has no rate limiter,skip send.",
          targetServiceInstance.getIp());
      return;
    }

    while (!caseItemArrayBlockingQueue.isEmpty()) {
      if (currentLimiter.failBreak()) {
        // remove interrupted target instance
        planExecutionContext.removeTargetInstance(targetServiceInstance);
        LOGGER.warn(
            "The current service instance - [{}],has reached the interruption threshold and the task has been stoped.",
            targetServiceInstance.getIp());
        break;
      }
      ReplayActionCaseItem caseItem = caseItemArrayBlockingQueue.poll();

      this.doExecute(caseItem, targetServiceInstance, groupSentLatch, planExecutionContext,
          currentLimiter);
    }
  }

  private void setServiceInstance(ReplayActionCaseItem replayActionCaseItem,
      ServiceInstance targetServiceInstance,
      PlanExecutionContext<?> planExecutionContext) {
    replayActionCaseItem.setTargetInstance(targetServiceInstance);

    Map<ServiceInstance, List<ServiceInstance>> bindMap = planExecutionContext.getBindInstanceMap();
    if (bindMap == null || bindMap.isEmpty()) {
      return;
    }
    List<ServiceInstance> sourceInstances = bindMap.get(targetServiceInstance);
    if (CollectionUtils.isNotEmpty(sourceInstances)) {
      int index = Math.abs(replayActionCaseItem.getId().hashCode() % sourceInstances.size());
      replayActionCaseItem.setSourceInstance(sourceInstances.get(index));
      LOGGER.info("The source instance is set to [{}], the target instance is set to [{}].",
          replayActionCaseItem.getSourceInstance().getIp(),
          replayActionCaseItem.getTargetInstance().getIp());
    }
  }

  private void doExecute(ReplayActionCaseItem replayActionCaseItem,
      ServiceInstance targetServiceInstance,
      CountDownLatch groupSentLatch,
      PlanExecutionContext<?> executionContext,
      SendSemaphoreLimiter sendSemaphoreLimiter) {

    if (replayActionCaseItem == null) {
      LOGGER.warn("The current case item is null,skip send.");
      return;
    }

    ReplayActionItem actionItem = replayActionCaseItem.getParent();
    MDCTracer.addDetailId(replayActionCaseItem.getId());

    try {
      ReplaySender replaySender = findReplaySender(replayActionCaseItem);
      if (replaySender == null) {
        groupSentLatch.countDown();
        doSendFailedAsFinish(replayActionCaseItem, CaseSendStatusType.READY_DEPENDENCY_FAILED);
        return;
      }
      this.setServiceInstance(replayActionCaseItem, targetServiceInstance, executionContext);
      sendSemaphoreLimiter.acquire();

      AsyncSendCaseTaskRunnable taskRunnable = new AsyncSendCaseTaskRunnable(this);
      taskRunnable.setExecutionStatus(executionContext.getExecutionStatus());
      taskRunnable.setCaseItem(replayActionCaseItem);
      taskRunnable.setReplaySender(replaySender);
      taskRunnable.setGroupSentLatch(groupSentLatch);
      taskRunnable.setLimiter(sendSemaphoreLimiter);
      taskRunnable.setMetricService(metricService);
      sendExecutorService.execute(taskRunnable);
      LOGGER.info("submit replay sending success");
    } catch (Throwable throwable) {
      groupSentLatch.countDown();
      sendSemaphoreLimiter.release(false);
      replayActionCaseItem.buildParentErrorMessage(throwable.getMessage());
      LOGGER.error("send group to remote host error:{} ,case item id:{}", throwable.getMessage(),
          replayActionCaseItem.getId(), throwable);
      doSendFailedAsFinish(replayActionCaseItem, CaseSendStatusType.EXCEPTION_FAILED);
    } finally {
      actionItem.recordProcessOne();
    }
  }

  @Override
  public void updateSendResult(ReplayActionCaseItem caseItem, CaseSendStatusType sendStatusType) {
    if (caseItem.getSourceResultId() == null) {
      caseItem.setSourceResultId(StringUtils.EMPTY);
    }
    if (caseItem.getTargetResultId() == null) {
      caseItem.setTargetResultId(StringUtils.EMPTY);
    }
    caseItem.setSendStatus(sendStatusType.getValue());

    if (sendStatusType == CaseSendStatusType.SUCCESS) {
      replayActionCaseItemRepository.updateSendResult(caseItem);
      // async compare task
      int compareDelaySeconds = defaultConfig.getConfigAsInt(
          COMPARE_DELAY_SECONDS, DEFAULT_COMPARE_DELAY_SECONDS);
      if (compareDelaySeconds == 0) {
        AsyncCompareCaseTaskRunnable compareTask = new AsyncCompareCaseTaskRunnable(
            replayResultComparer, caseItem);
        compareExecutorService.execute(compareTask);
      } else {
        AsyncDelayCompareCaseTaskRunnable delayCompareTask = new AsyncDelayCompareCaseTaskRunnable(
            replayCompareService, caseItem);
        compareScheduleExecutorService.schedule(delayCompareTask,
            compareDelaySeconds, TimeUnit.SECONDS);
      }
      LOGGER.info("Async compare task distributed, case id: {}, recordId: {}", caseItem.getId(),
          caseItem.getRecordId());
    } else {
      doSendFailedAsFinish(caseItem, sendStatusType);
    }
  }

  private ReplaySender findReplaySender(ReplayActionCaseItem caseItem) {
    ReplaySender sender = senderFactory.findReplaySender(caseItem.getCaseType());
    if (sender != null) {
      return sender;
    }
    LOGGER.warn("find empty ReplaySender for detailId: {}, caseType:{}", caseItem.getId(),
        caseItem.getCaseType());
    return null;
  }

  private void markAllSendStatus(List<ReplayActionCaseItem> sourceItemList,
      CaseSendStatusType sendStatusType) {
    for (ReplayActionCaseItem caseItem : sourceItemList) {
      doSendFailedAsFinish(caseItem, sendStatusType);
    }
  }

  @Override
  public void doSendFailedAsFinish(ReplayActionCaseItem caseItem,
      CaseSendStatusType sendStatusType) {
    try {
      LOGGER.info("try do send failed as finish case id: {} ,send status:{}", caseItem.getId(),
          sendStatusType);
      caseItem.setSendStatus(sendStatusType.getValue());
      if (caseItem.getSourceResultId() == null) {
        caseItem.setSourceResultId(StringUtils.EMPTY);
      }
      if (caseItem.getTargetResultId() == null) {
        caseItem.setTargetResultId(StringUtils.EMPTY);
      }
      boolean updateResult = replayActionCaseItemRepository.updateSendResult(caseItem);
      String errorMessage = caseItem.getSendErrorMessage();
      if (StringUtils.isEmpty(errorMessage)) {
        errorMessage = sendStatusType.name();
      }
      comparisonWriter.writeIncomparable(caseItem, errorMessage);
      progressTracer.finishOne(caseItem);
      LOGGER.info("do send failed as finish success case id: {},updateResult:{}", caseItem.getId(),
          updateResult);
    } catch (Throwable throwable) {
      LOGGER.error("doSendFailedAsFinish error:{}", throwable.getMessage(), throwable);
    }

  }
}
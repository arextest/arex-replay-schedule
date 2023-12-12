package com.arextest.schedule.service.noise;

import com.arextest.schedule.bizlog.BizLogger;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.common.SendSemaphoreLimiter;
import com.arextest.schedule.comparer.CompareConfigService;
import com.arextest.schedule.comparer.ComparisonWriter;
import com.arextest.schedule.comparer.CustomComparisonConfigurationHandler;
import com.arextest.schedule.comparer.InvalidReplayCaseService;
import com.arextest.schedule.comparer.impl.DefaultReplayResultComparer;
import com.arextest.schedule.comparer.impl.PrepareCompareSourceRemoteLoader;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.dao.mongodb.ReplayCompareResultRepositoryImpl;
import com.arextest.schedule.dao.mongodb.ReplayNoiseRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanActionRepository;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.CompareModeType;
import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.noiseidentify.ActionItemForNoiseIdentify;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.sender.ReplaySender;
import com.arextest.schedule.sender.ReplaySenderFactory;
import com.arextest.schedule.service.MetricService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.beans.BeanUtils;

/**
 * Created by coryhh on 2023/10/17.
 */
@Slf4j
public class ReplayNoiseIdentifyService implements ReplayNoiseIdentify {

  private static final int CASE_COUNT_FOR_NOISE_IDENTIFY = 2;
  ExecutorService sendExecutorService;
  ExecutorService analysisNoiseExecutorService;
  private ReplaySenderFactory replaySenderFactory;
  private PrepareCompareSourceRemoteLoader sourceRemoteLoader;
  private DefaultReplayResultComparer defaultResultComparer;
  private ReplayCompareResultRepositoryImpl replayCompareResultRepository;
  private ReplayNoiseRepository replayNoiseRepository;
  private ReplayPlanActionRepository replayPlanActionRepository;

  public ReplayNoiseIdentifyService(CompareConfigService compareConfigService,
      ProgressTracer progressTracer,
      ComparisonWriter comparisonOutputWriter, ReplayActionCaseItemRepository caseItemRepository,
      MetricService metricService,
      CustomComparisonConfigurationHandler customComparisonConfigurationHandler,
      ReplayCompareResultRepositoryImpl replayCompareResultRepository,
      ReplayNoiseRepository replayNoiseRepository,
      ReplayPlanActionRepository replayPlanActionRepository,
      ReplaySenderFactory replaySenderFactory,
      PrepareCompareSourceRemoteLoader sourceRemoteLoader, ExecutorService sendExecutorService,
      ExecutorService analysisNoiseExecutorService, InvalidReplayCaseService invalidReplayCaseService) {
    this.defaultResultComparer =
        new DefaultReplayResultComparer(compareConfigService, sourceRemoteLoader, progressTracer,
            comparisonOutputWriter, caseItemRepository, metricService,
            customComparisonConfigurationHandler,invalidReplayCaseService);
    this.replayCompareResultRepository = replayCompareResultRepository;
    this.replayNoiseRepository = replayNoiseRepository;
    this.replayPlanActionRepository = replayPlanActionRepository;
    this.replaySenderFactory = replaySenderFactory;
    this.sourceRemoteLoader = sourceRemoteLoader;
    this.sendExecutorService = sendExecutorService;
    this.analysisNoiseExecutorService = analysisNoiseExecutorService;
  }

  @Override
  public void noiseIdentify(List<ReplayActionCaseItem> allCasesOfContext,
      PlanExecutionContext<?> executionContext) {

    Map<ReplayActionItem, List<ReplayActionCaseItem>> actionsOfBatch =
        allCasesOfContext.stream().collect(Collectors.groupingBy(ReplayActionCaseItem::getParent));

    String contextName = executionContext.getContextName();
    List<MutablePair<ReplayActionItem, List<ReplayActionCaseItem>>> casesForNoise = new ArrayList<>();
    for (Map.Entry<ReplayActionItem, List<ReplayActionCaseItem>> actionItemListEntry : actionsOfBatch.entrySet()) {
      ReplayActionItem action = actionItemListEntry.getKey();
      List<ReplayActionCaseItem> cases = actionItemListEntry.getValue();

      // if the context has been noise analyzed, skip it
      if (action.getNoiseFinishedContexts() != null
          && action.getNoiseFinishedContexts().containsKey(contextName)) {
        continue;
      }

      int caseSize = cases.size();
      int tempCount = 0;
      List<ReplayActionCaseItem> tempCases = new ArrayList<>();

      ReplayActionItem targetAction = new ReplayActionItem();
      BeanUtils.copyProperties(action, targetAction);
      targetAction.setSourceInstance(targetAction.getTargetInstance());

      while (tempCount < CASE_COUNT_FOR_NOISE_IDENTIFY && tempCount < caseSize) {
        ReplayActionCaseItem sourceCase = cases.get(tempCount);
        ReplayActionCaseItem targetCase = new ReplayActionCaseItem();
        BeanUtils.copyProperties(sourceCase, targetCase);
        targetCase.setParent(targetAction);
        targetCase.setCompareMode(CompareModeType.FULL);
        tempCases.add(targetCase);
        tempCount++;
      }
      casesForNoise.add(new MutablePair<>(action, tempCases));
    }

    // sync wait for all cases sending to complete
    long start = System.currentTimeMillis();
    SendSemaphoreLimiter limiter = executionContext.getExecutionStatus().getLimiter();
    List<ReplayActionCaseItem> replayActionCaseItems = casesForNoise.stream()
        .map(MutablePair::getRight)
        .flatMap(List::stream).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    CreateReplayNoiseSendTaskRequest request = new CreateReplayNoiseSendTaskRequest(
        replayActionCaseItems, limiter);
    BizLogger.recordCaseForNoiseSendStart(executionContext, replayActionCaseItems.size());
    this.doReplayNoiseSendTasks(request);
    BizLogger.recordCaseForNoiseSendFinish(executionContext, replayActionCaseItems.size(),
        System.currentTimeMillis() - start);
    limiter.reset();

    // async analysis
    for (MutablePair<ReplayActionItem, List<ReplayActionCaseItem>> itemPair : casesForNoise) {
      ActionItemForNoiseIdentify actionItemForNoiseIdentify =
          this.getActionItemForNoiseIdentify(itemPair, contextName);
      this.analysisNoise(actionItemForNoiseIdentify);
    }
  }

  @Override
  public void rerunNoiseAnalysisRecovery(List<ReplayActionItem> actionItems) {
    if (CollectionUtils.isEmpty(actionItems)) {
      return;
    }

    for (ReplayActionItem actionItem : actionItems) {
      Map<String, Integer> noiseFinishedContexts = actionItem.getNoiseFinishedContexts();
      List<ReplayActionCaseItem> caseItemList = actionItem.getCaseItemList();
      if (CollectionUtils.isEmpty(caseItemList)) {
        continue;
      }
      Set<String> failedCaseIdentifiers =
          caseItemList.stream().map(ReplayActionCaseItem::getContextIdentifier)
              .collect(Collectors.toSet());
      if (noiseFinishedContexts != null) {
        for (String failedCaseIdentifier : failedCaseIdentifiers) {
          noiseFinishedContexts.remove(PlanExecutionContext.buildContextName(failedCaseIdentifier));
        }
      }
    }
    replayPlanActionRepository.bulkUpdateNoiseFinishedContexts(actionItems);
    List<String> failedActions = actionItems.stream().map(ReplayActionItem::getId)
        .collect(Collectors.toList());
    replayNoiseRepository.removeReplayNoise(failedActions);
  }

  private ActionItemForNoiseIdentify getActionItemForNoiseIdentify(
      MutablePair<ReplayActionItem, List<ReplayActionCaseItem>> itemPair, String contextName) {
    ReplayActionItem replayActionItem = itemPair.getLeft();
    List<ReplayActionCaseItem> caseItemList = itemPair.getRight();

    ActionItemForNoiseIdentify actionItemForNoiseIdentify = new ActionItemForNoiseIdentify();
    actionItemForNoiseIdentify.setPlanId(replayActionItem.getPlanId());
    actionItemForNoiseIdentify.setPlanItemId(replayActionItem.getId());
    actionItemForNoiseIdentify.setContextName(contextName);
    actionItemForNoiseIdentify.setCases(caseItemList);
    return actionItemForNoiseIdentify;
  }

  private void analysisNoise(ActionItemForNoiseIdentify actionItemForNoiseIdentify) {
    AsyncNoiseCaseAnalysisTaskRunnable asyncNoiseCaseAnalysisTaskRunnable =
        new AsyncNoiseCaseAnalysisTaskRunnable();
    asyncNoiseCaseAnalysisTaskRunnable.setActionItemForNoiseIdentify(actionItemForNoiseIdentify);
    asyncNoiseCaseAnalysisTaskRunnable.setSourceRemoteLoader(sourceRemoteLoader);
    asyncNoiseCaseAnalysisTaskRunnable.setDefaultResultComparer(defaultResultComparer);
    asyncNoiseCaseAnalysisTaskRunnable.setReplayCompareResultRepository(
        replayCompareResultRepository);
    asyncNoiseCaseAnalysisTaskRunnable.setReplayNoiseRepository(replayNoiseRepository);
    asyncNoiseCaseAnalysisTaskRunnable.setReplayPlanActionRepository(replayPlanActionRepository);
    CompletableFuture.runAsync(asyncNoiseCaseAnalysisTaskRunnable, analysisNoiseExecutorService);
  }

  private void doReplayNoiseSendTasks(CreateReplayNoiseSendTaskRequest request) {

    List<ReplayActionCaseItem> cases = request.getCases();
    if (CollectionUtils.isEmpty(cases)) {
      return;
    }

    SendSemaphoreLimiter limiter = request.getLimiter();
    int caseSize = cases.size();
    CountDownLatch countDownLatch = new CountDownLatch(caseSize);
    for (int i = 0; i < caseSize; i++) {
      ReplayActionCaseItem caseItem = cases.get(i);
      MDCTracer.addNoiseActionId(caseItem.getPlanItemId());
      MDCTracer.addNoiseDetailId(caseItem.getId());
      try {
        ReplaySender replaySender = replaySenderFactory.findReplaySender(caseItem.getCaseType());
        if (replaySender == null) {
          countDownLatch.countDown();
          LOGGER.error("replay sender not found,case item id:{}", caseItem.getId());
          continue;
        }
        // acquire semaphore, control the number of concurrent requests
        limiter.acquire();
        AsyncNoiseCaseSendTaskRunnable taskRunnable =
            new AsyncNoiseCaseSendTaskRunnable(replaySender, limiter, countDownLatch, caseItem);
        sendExecutorService.execute(taskRunnable);
      } catch (RuntimeException exception) {
        // when happen runtime exception, we should release the semaphore; back-to-back logic
        limiter.release(false);
        countDownLatch.countDown();
        LOGGER.error("send case for noise analysis error:{}", exception.getMessage(), exception);
      }
    }

    // await all request of batch
    try {
      boolean await = countDownLatch.await(CommonConstant.GROUP_SENT_WAIT_TIMEOUT_SECONDS,
          TimeUnit.SECONDS);
      if (!await) {
        LOGGER.error("failed to await all request of batch");
      }
    } catch (InterruptedException e) {
      LOGGER.error("send case for noise analysis error:{}", e.getMessage(), e);
      Thread.currentThread().interrupt();
    }
    MDCTracer.removeNoiseActionId();
    MDCTracer.removeNoiseDetailId();
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  private static class CreateReplayNoiseSendTaskRequest {

    private List<ReplayActionCaseItem> cases;

    private SendSemaphoreLimiter limiter;

  }

}

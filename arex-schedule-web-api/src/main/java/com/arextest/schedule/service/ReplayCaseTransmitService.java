package com.arextest.schedule.service;

import com.arextest.model.mock.Mocker;
import com.arextest.schedule.bizlog.BizLogger;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.common.SendSemaphoreLimiter;
import com.arextest.schedule.comparer.ComparisonWriter;
import com.arextest.schedule.comparer.ReplayResultComparer;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.mdc.AbstractTracedRunnable;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.*;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.sender.ReplaySender;
import com.arextest.schedule.sender.ReplaySenderFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author jmo
 * @since 2021/9/13
 */
@Service
@Slf4j
public class ReplayCaseTransmitService {
    @Resource
    private ExecutorService sendExecutorService;
    @Resource
    private ExecutorService compareExecutorService;
    private static final int ACTIVE_SERVICE_RETRY_COUNT = 3;
    private static final int GROUP_SENT_WAIT_TIMEOUT = 500;
    @Resource
    private ReplayResultComparer replayResultComparer;
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

    public void send(ReplayActionItem replayActionItem) {
        List<ReplayActionCaseItem> sourceItemList = replayActionItem.getCaseItemList();
        if (CollectionUtils.isEmpty(sourceItemList)) {
            return;
        }

        // warmUp should be done once for each endpoint
        if (!replayActionItem.isItemProcessed()) {
            activeRemoteHost(sourceItemList);
        }

        // checkpoint: after JIT warm up, before sending page of cases
        if (replayActionItem.getPlanStatus().isInterrupted()) {
            return;
        }

        if (replayActionItem.getPlanStatus().isCanceled()) {
            progressEvent.onActionCancelled(replayActionItem);
            return;
        }

        try {
            doSendValuesToRemoteHost(sourceItemList, replayActionItem.getPlanStatus());
        } catch (Throwable throwable) {
            LOGGER.error("do send error:{}", throwable.getMessage(), throwable);
            markAllSendStatus(sourceItemList, CaseSendStatusType.EXCEPTION_FAILED);
        } finally {
            replayActionItem.recordProcessCaseCount(sourceItemList.size());
        }
    }

    @SuppressWarnings("rawtypes")
    public void releaseCasesOfContext(ReplayActionItem replayActionItem, PlanExecutionContext executionContext) {
        // checkpoint: before skipping batch of group
        if (executionContext.getExecutionStatus().isAbnormal()) {
            return;
        }

        int contextCasesCount = (int) replayActionCaseItemRepository.countWaitingSendList(replayActionItem.getId(),
                executionContext.getContextCaseQuery());

        replayActionItem.recordProcessCaseCount(contextCasesCount);
        replayActionItem.getSendRateLimiter().batchRelease(false, contextCasesCount);

        // if we skip the rest of cases remaining in the action item, set its status
        if (Integer.valueOf(replayActionItem.getReplayCaseCount()).equals(replayActionItem.getCaseProcessCount())) {
            progressEvent.onActionInterrupted(replayActionItem);
            ReplayPlan replayPlan = replayActionItem.getParent();
            for (int i = 0; i < contextCasesCount; i++) {
                progressTracer.finishCaseByPlan(replayPlan);
            }
        } else {
            for (int i = 0; i < contextCasesCount; i++) {
                progressTracer.finishCaseByAction(replayActionItem);
            }
        }
        BizLogger.recordContextSkipped(executionContext, replayActionItem, contextCasesCount);
    }

    private ReplayActionCaseItem cloneCaseItem(List<ReplayActionCaseItem> groupValues, int index) {
        ReplayActionCaseItem caseItem = new ReplayActionCaseItem();
        ReplayActionCaseItem source = groupValues.get(index);
        caseItem.setRecordId(source.getRecordId());
        caseItem.setTargetResultId(source.getTargetResultId());
        caseItem.setCaseType(source.getCaseType());
        caseItem.setParent(source.getParent());
        caseItem.setTargetRequest(cloneTargetRequest(source.getTargetRequest()));
        caseItem.setSourceResultId(source.getSourceResultId());
        caseItem.setPlanItemId(source.getPlanItemId());
        return caseItem;
    }

    private Mocker.Target cloneTargetRequest(Mocker.Target targetRequest) {
        ObjectMapper objectMapper = new ObjectMapper();
        Mocker.Target newTarget = null;
        try {
            String oldTargetRequest = objectMapper.writeValueAsString(targetRequest);
            newTarget = objectMapper.readValue(oldTargetRequest, Mocker.Target.class);
        } catch (JsonProcessingException e) {
            LOGGER.error("cloneTargetRequest item error:{}", e.getMessage());
        }
        return newTarget;
    }


    private void doSendValuesToRemoteHost(List<ReplayActionCaseItem> values, ExecutionStatus executionStatus) {
        final int valueSize = values.size();
        final ReplayActionCaseItem caseItem = values.get(0);
        final ReplayActionItem actionItem = caseItem.getParent();
        final SendSemaphoreLimiter semaphore = actionItem.getSendRateLimiter();
        final CountDownLatch groupSentLatch = new CountDownLatch(valueSize);

        for (int i = 0; i < valueSize; i++) {
            ReplayActionCaseItem replayActionCaseItem = values.get(i);
            MDCTracer.addDetailId(caseItem.getId());
            // checkpoint: before init case runnable
            if (executionStatus.isAbnormal()) {
                LOGGER.info("replay interrupted,case item id:{}", replayActionCaseItem.getId());
                MDCTracer.removeDetailId();
                return;
            }

            try {
                ReplaySender replaySender = findReplaySender(replayActionCaseItem);
                if (replaySender == null) {
                    groupSentLatch.countDown();
                    doSendFailedAsFinish(replayActionCaseItem, CaseSendStatusType.READY_DEPENDENCY_FAILED);
                    continue;
                }
                semaphore.acquire();
                AsyncSendCaseTaskRunnable taskRunnable = new AsyncSendCaseTaskRunnable(this);
                taskRunnable.setExecutionStatus(executionStatus);
                taskRunnable.setCaseItem(replayActionCaseItem);
                taskRunnable.setReplaySender(replaySender);
                taskRunnable.setGroupSentLatch(groupSentLatch);
                taskRunnable.setLimiter(semaphore);
                taskRunnable.setMetricService(metricService);
                sendExecutorService.execute(taskRunnable);
                LOGGER.info("submit replay sending success");
            } catch (Throwable throwable) {
                groupSentLatch.countDown();
                semaphore.release(false);
                replayActionCaseItem.buildParentErrorMessage(throwable.getMessage());
                LOGGER.error("send group to remote host error:{} ,case item id:{}", throwable.getMessage(),
                        replayActionCaseItem.getId(), throwable);
                doSendFailedAsFinish(replayActionCaseItem, CaseSendStatusType.EXCEPTION_FAILED);
            }
        }

        try {
            boolean clear = groupSentLatch.await(GROUP_SENT_WAIT_TIMEOUT, TimeUnit.SECONDS);
            if (!clear) {
                LOGGER.error("Send group failed to await all request of batch");
            }
        } catch (InterruptedException e) {
            LOGGER.error("send group to remote host error:{}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
        MDCTracer.removeDetailId();
    }

    void updateSendResult(ReplayActionCaseItem caseItem, CaseSendStatusType sendStatusType) {
        if (caseItem.getSourceResultId() == null) {
            caseItem.setSourceResultId(StringUtils.EMPTY);
        }
        if (caseItem.getTargetResultId() == null) {
            caseItem.setTargetResultId(StringUtils.EMPTY);
        }
        caseItem.setSendStatus(sendStatusType.getValue());
        if (sendStatusType == CaseSendStatusType.SUCCESS) {
            AsyncCompareCaseTaskRunnable compareTask = new AsyncCompareCaseTaskRunnable(caseItem);
            compareExecutorService.execute(compareTask);
            LOGGER.info("Async compare task distributed, case id: {}", caseItem.getId());
        } else {
            doSendFailedAsFinish(caseItem, sendStatusType);
        }
    }

    private ReplaySender findReplaySender(ReplayActionCaseItem caseItem) {
        ReplaySender sender = senderFactory.findReplaySender(caseItem.getCaseType());
        if (sender != null) {
            return sender;
        }
        LOGGER.warn("find empty ReplaySender for detailId: {}, caseType:{}", caseItem.getId(), caseItem.getCaseType());
        return null;
    }

    private void activeRemoteHost(List<ReplayActionCaseItem> sourceItemList) {
        try {
            for (int i = 0; i < ACTIVE_SERVICE_RETRY_COUNT && i < sourceItemList.size(); i++) {
                ReplayActionCaseItem caseItem = cloneCaseItem(sourceItemList, i);
                ReplaySender replaySender = findReplaySender(caseItem);
                if (replaySender == null) {
                    continue;
                }
                if (replaySender.activeRemoteService(caseItem)) {
                    return;
                }
                Thread.sleep(CommonConstant.THREE_SECOND_MILLIS);
            }
        } catch (Exception ex) {
            LOGGER.error("active remote host error", ex);
            Thread.currentThread().interrupt();
        }
    }

    private void markAllSendStatus(List<ReplayActionCaseItem> sourceItemList, CaseSendStatusType sendStatusType) {
        for (ReplayActionCaseItem caseItem : sourceItemList) {
            doSendFailedAsFinish(caseItem, sendStatusType);
        }
    }

    private void doSendFailedAsFinish(ReplayActionCaseItem caseItem, CaseSendStatusType sendStatusType) {
        try {
            LOGGER.info("try do send failed as finish case id: {} ,send status:{}", caseItem.getId(), sendStatusType);
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
            LOGGER.info("do send failed as finish success case id: {},updateResult:{}", caseItem.getId(), updateResult);
        } catch (Throwable throwable) {
            LOGGER.error("doSendFailedAsFinish error:{}", throwable.getMessage(), throwable);
        }

    }


    private class AsyncCompareCaseTaskRunnable extends AbstractTracedRunnable {
        private ReplayActionCaseItem caseItem;

        AsyncCompareCaseTaskRunnable(ReplayActionCaseItem caseItem) {
            this.caseItem = caseItem;
        }

        @Override
        protected void doWithTracedRunning() {
            boolean result = replayResultComparer.compare(caseItem, true);
            if (result) {
                replayActionCaseItemRepository.updateSendResult(caseItem);
            } else {
                LOGGER.error("Comparer returned false, case id: {}", caseItem.getId());
            }
        }
    }
}
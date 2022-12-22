package com.arextest.schedule.service;

import com.arextest.common.cache.CacheProvider;
import com.arextest.model.mock.Mocker;
import com.arextest.schedule.comparer.ComparisonWriter;
import com.arextest.schedule.comparer.ReplayResultComparer;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.common.SendSemaphoreLimiter;
import com.arextest.schedule.model.CaseSendStatusType;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.sender.ReplaySender;
import com.arextest.schedule.sender.ReplaySenderFactory;
import com.arextest.schedule.common.CommonConstant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.arextest.schedule.common.CommonConstant.STOP_PLAN_REDIS_KEY;

/**
 * @author jmo
 * @since 2021/9/13
 */
@Service
@Slf4j
public class ReplayCaseTransmitService {
    @Resource
    private ExecutorService sendExecutorService;
    private static final int ACTIVE_SERVICE_RETRY_COUNT = 3;
    private static final int GROUP_SENT_WAIT_TIMEOUT = 300;
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
    private CacheProvider redisCacheProvider;
    @Resource
    private ProgressEvent progressEvent;

    public boolean send(ReplayActionItem replayActionItem) {
        List<ReplayActionCaseItem> sourceItemList = replayActionItem.getCaseItemList();
        if (CollectionUtils.isEmpty(sourceItemList)) {
            return false;
        }
        activeRemoteHost(sourceItemList);
        Map<String, List<ReplayActionCaseItem>> versionGroupedResult = groupByDependencyVersion(sourceItemList);
        LOGGER.info("found replay send size of group: {}", versionGroupedResult.size());
        replayActionItem.getSendRateLimiter().reset();
        byte[] cancelKey = getCancelKey(replayActionItem.getPlanId());
        for (Map.Entry<String, List<ReplayActionCaseItem>> versionEntry : versionGroupedResult.entrySet()) {
            List<ReplayActionCaseItem> groupValues = versionEntry.getValue();
            if (replayActionItem.getSendRateLimiter().failBreak()) {
                break;
            }
            if (isCancelled(cancelKey)) {
                progressEvent.onActionCancelled(replayActionItem);
                return true;
            }
            try {
                if (StringUtils.isEmpty(versionEntry.getKey())) {
                    doSendValuesToRemoteHost(groupValues);
                } else {
                    doSendWithDependencyToRemoteHost(groupValues);
                }
            } catch (Throwable throwable) {
                LOGGER.error("do send error:{} , group key: {}", versionEntry.getKey(), throwable.getMessage()
                        , throwable);
                markAllSendStatus(groupValues, CaseSendStatusType.EXCEPTION_FAILED);
            }
        }
        return false;
    }

    private byte[] getCancelKey(String planId) {
        return (STOP_PLAN_REDIS_KEY + planId).getBytes(StandardCharsets.UTF_8);
    }

    private boolean isCancelled(byte[] rediskey) {
        byte[] bytes = redisCacheProvider.get(rediskey);
        if (bytes == null) {
            return false;
        }
        return true;
    }

    private void doSendWithDependencyToRemoteHost(List<ReplayActionCaseItem> groupValues) {
        try {
            ReplayActionCaseItem dependSource = cloneCaseItem(groupValues, 0);
            if (prepareRemoteDependency(dependSource)) {
                Thread.sleep(CommonConstant.THREE_SECOND_MILLIS);
                doSendValuesToRemoteHost(groupValues);
            } else {
                markAllSendStatus(groupValues, CaseSendStatusType.READY_DEPENDENCY_FAILED);
                LOGGER.error("prepare remote dependency false, group key: {} marked failed ,action id:{}",
                        groupValues.get(0).replayDependency(),
                        groupValues.get(0).getParent().getId());
            }
        } catch (Exception e) {
            LOGGER.error("do send with dependency to remote host error ,action id:{}",
                    groupValues.get(0).getParent().getId(), e);
        }
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


    private void doSendValuesToRemoteHost(List<ReplayActionCaseItem> values) {
        final int valueSize = values.size();
        final ReplayActionCaseItem caseItem = values.get(0);
        final ReplayActionItem actionItem = caseItem.getParent();
        final SendSemaphoreLimiter semaphore = actionItem.getSendRateLimiter();
        final CountDownLatch groupSentLatch = new CountDownLatch(valueSize);
        for (int i = 0; i < valueSize; i++) {
            ReplayActionCaseItem replayActionCaseItem = values.get(i);
            MDCTracer.addDetailId(caseItem.getId());
            try {
                ReplaySender replaySender = findReplaySender(replayActionCaseItem);
                if (replaySender == null) {
                    groupSentLatch.countDown();
                    doSendFailedAsFinish(replayActionCaseItem, CaseSendStatusType.READY_DEPENDENCY_FAILED);
                    continue;
                }
                if (semaphore.failBreak()) {
                    LOGGER.info("replay interrupted,case item id:{}", replayActionCaseItem.getId());
                    MDCTracer.removeDetailId();
                    return;
                }
                semaphore.acquire();
                AsyncSendCaseTaskRunnable taskRunnable = new AsyncSendCaseTaskRunnable(this);
                taskRunnable.setCaseItem(replayActionCaseItem);
                taskRunnable.setReplaySender(replaySender);
                taskRunnable.setGroupSentLatch(groupSentLatch);
                taskRunnable.setLimiter(semaphore);
                sendExecutorService.execute(taskRunnable);
                LOGGER.info("submit replay sending success");
            } catch (Throwable throwable) {
                groupSentLatch.countDown();
                semaphore.release(false);
                LOGGER.error("send group to remote host error:{} ,case item id:{}", throwable.getMessage(),
                        replayActionCaseItem.getId(), throwable);
                doSendFailedAsFinish(replayActionCaseItem, CaseSendStatusType.EXCEPTION_FAILED);
            }
        }
        MDCTracer.removeDetailId();
        try {
            groupSentLatch.await(GROUP_SENT_WAIT_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("send group to remote host error:{}", e.getMessage(), e);
        }
    }

    void updateSendResult(ReplayActionCaseItem caseItem, CaseSendStatusType sendStatusType) {
        if (caseItem.getSourceResultId() == null) {
            caseItem.setSourceResultId(StringUtils.EMPTY);
        }
        if (caseItem.getTargetResultId() == null) {
            caseItem.setTargetResultId(StringUtils.EMPTY);
        }
        caseItem.setSendStatus(sendStatusType.getValue());
        if (sendStatusType == CaseSendStatusType.SUCCESS && replayResultComparer.compare(caseItem, true)) {
            replayActionCaseItemRepository.updateSendResult(caseItem);
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

    private boolean prepareRemoteDependency(ReplayActionCaseItem caseItem) {
        String replayDependency = caseItem.replayDependency();
        boolean prepareResult = false;
        ReplaySender replaySender = findReplaySender(caseItem);
        if (replaySender != null) {
            prepareResult = replaySender.prepareRemoteDependency(caseItem);
        }
        LOGGER.info("prepare remote dependency version: {} , result: {} , {} -> {}", replayDependency, prepareResult,
                caseItem.getParent().getServiceName(), caseItem.getParent().getOperationName());
        return prepareResult;
    }

    private void activeRemoteHost(List<ReplayActionCaseItem> sourceItemList) {
        try {
            for (int i = 0; i < ACTIVE_SERVICE_RETRY_COUNT && i < sourceItemList.size(); i++) {
                ReplayActionCaseItem caseItem = sourceItemList.get(i);
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

    private Map<String, List<ReplayActionCaseItem>> groupByDependencyVersion(List<ReplayActionCaseItem> sourceItemList) {
        if (this.skipDependencyGroupRequested(sourceItemList)) {
            return Collections.singletonMap(getReplayDependencyKey(sourceItemList.get(0).replayDependency()),
                    sourceItemList);
        }
        SortedMap<String, List<ReplayActionCaseItem>> groupResult = Maps.newTreeMap();
        for (ReplayActionCaseItem replayActionCaseItem : sourceItemList) {
            List<ReplayActionCaseItem> values =
                    groupResult.computeIfAbsent(getReplayDependencyKey(replayActionCaseItem.replayDependency()),
                            (key) -> new ArrayList<>());
            values.add(replayActionCaseItem);
        }

        return groupResult;
    }

    private boolean skipDependencyGroupRequested(List<ReplayActionCaseItem> sourceItemList) {
        for (int i = 1; i < sourceItemList.size(); i++) {
            if (!StringUtils.equals(sourceItemList.get(i).replayDependency(),
                    sourceItemList.get(i - 1).replayDependency())) {
                return false;
            }
        }
        return true;
    }

    private String getReplayDependencyKey(String key) {
        String intkey = "";
        if (StringUtils.isNotBlank(key)) {
            intkey = key;
        }
        return intkey;
    }
}
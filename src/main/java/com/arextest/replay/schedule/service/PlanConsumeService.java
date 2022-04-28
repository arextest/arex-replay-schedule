package com.arextest.replay.schedule.service;

import com.arextest.replay.schedule.dao.ReplayActionCaseItemRepository;
import com.arextest.replay.schedule.dao.ReplayPlanRepository;
import com.arextest.replay.schedule.mdc.AbstractTracedRunnable;
import com.arextest.replay.schedule.mdc.MDCTracer;
import com.arextest.replay.schedule.progress.ProgressEvent;
import com.arextest.replay.schedule.progress.ProgressTracer;
import com.arextest.replay.schedule.common.CommonConstant;
import com.arextest.replay.schedule.common.SendSemaphoreLimiter;
import com.arextest.replay.schedule.model.CaseSendStatusType;
import com.arextest.replay.schedule.model.ReplayActionCaseItem;
import com.arextest.replay.schedule.model.ReplayActionItem;
import com.arextest.replay.schedule.model.ReplayPlan;
import com.arextest.replay.schedule.model.ReplayStatusType;
import com.arextest.replay.schedule.utils.ReplayParentBinder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author jmo
 * @since 2021/9/15
 */
@Slf4j
@Service
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
    private ReplayPlanRepository replayPlanRepository;
    @Resource
    private ProgressTracer progressTracer;
    @Resource
    private ProgressEvent progressEvent;

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
        protected void doWithTracedRunning() {
            saveActionCaseToSend(replayPlan);
        }
    }

    private void saveActionCaseToSend(ReplayPlan replayPlan) {
        int planSavedCaseSize = saveAllActionCase(replayPlan.getReplayActionItemList());
        if (planSavedCaseSize != replayPlan.getCaseTotalCount()) {
            LOGGER.info("update the plan TotalCount, plan id:{} ,appId: {} , size: {} -> {}", replayPlan.getId(),
                    replayPlan.getAppId(), replayPlan.getCaseTotalCount(), planSavedCaseSize);
            replayPlan.setCaseTotalCount(planSavedCaseSize);
            replayPlanRepository.updateCaseTotal(replayPlan.getId(), planSavedCaseSize);
        }
        this.sendAllActionCase(replayPlan);
        if (planSavedCaseSize == 0) {
            progressEvent.onReplayPlanFinish(replayPlan);
        }
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

    private void sendAllActionCase(ReplayPlan replayPlan) {
        progressTracer.initTotal(replayPlan);
        final SendSemaphoreLimiter sendRateLimiter = new SendSemaphoreLimiter();
        sendRateLimiter.setTotalTasks(replayPlan.getCaseTotalCount());
        sendRateLimiter.setSendMaxRate(replayPlan.getReplaySendMaxQps());
        boolean isInterrupted = false;
        for (ReplayActionItem replayActionItem : replayPlan.getReplayActionItemList()) {
            MDCTracer.addActionId(replayActionItem.getId());
            if (replayActionItem.finished()) {
                continue;
            }
            if (replayActionItem.isEmpty()) {
                replayActionItem.setReplayFinishTime(new Date());
                progressEvent.onActionComparisonFinish(replayActionItem);
                continue;
            }
            if (isInterrupted) {
                progressEvent.onActionInterrupted(replayActionItem);
                continue;
            }
            if (replayActionItem.getReplayFinishTime() == null) {
                progressEvent.onActionBeforeSend(replayActionItem);
            }
            replayActionItem.setSendRateLimiter(sendRateLimiter);
            sendByPaging(replayActionItem);
            isInterrupted = sendRateLimiter.failBreak();
            if (isInterrupted) {
                progressEvent.onActionInterrupted(replayActionItem);
                continue;
            }
            progressEvent.onActionAfterSend(replayActionItem);
        }
        if (isInterrupted) {
            progressEvent.onReplayPlanFinish(replayPlan, ReplayStatusType.FAIL_INTERRUPTED);
            LOGGER.info("The plan was interrupted, plan id:{} ,appId: {} ", replayPlan.getId(),
                    replayPlan.getAppId());
            return;
        }
        LOGGER.info("All the plan action sent,waiting to compare, plan id:{} ,appId: {} ", replayPlan.getId(),
                replayPlan.getAppId());
    }

    private void sendByPaging(ReplayActionItem replayActionItem) {
        List<ReplayActionCaseItem> sourceItemList;
        while (true) {
            sourceItemList = replayActionCaseItemRepository.waitingSendList(replayActionItem.getId(),
                    CommonConstant.MAX_PAGE_SIZE);
            replayActionItem.setCaseItemList(sourceItemList);
            if (CollectionUtils.isEmpty(sourceItemList)) {
                break;
            }
            ReplayParentBinder.setupCaseItemParent(sourceItemList, replayActionItem);
            replayCaseTransmitService.send(replayActionItem);
            if (replayActionItem.getSendRateLimiter().failBreak()) {
                break;
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
        int size = 0;
        for (int i = 0; i < caseItemList.size(); i++) {
            ReplayActionCaseItem caseItem = caseItemList.get(i);
            ReplayActionCaseItem viewReplay = caseRemoteLoadService.viewReplayLoad(caseItem);
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

    private int doPagingLoadCaseSave(ReplayActionItem replayActionItem) {
        final ReplayPlan replayPlan = replayActionItem.getParent();
        long beginTimeMills = replayActionItem.getLastRecordTime();
        if (beginTimeMills == 0) {
            beginTimeMills = replayPlan.getCaseSourceFrom().getTime();
        }
        long endTimeMills = replayPlan.getCaseSourceTo().getTime();
        int size = 0;
        while (beginTimeMills < endTimeMills) {
            List<ReplayActionCaseItem> caseItemList = caseRemoteLoadService.pagingLoad(beginTimeMills, endTimeMills,
                    replayActionItem);
            if (CollectionUtils.isEmpty(caseItemList) || size > CommonConstant.OPERATION_MAX_CASE_COUNT) {
                break;
            } else {
                ReplayParentBinder.setupCaseItemParent(caseItemList, replayActionItem);
                size += caseItemList.size();
                beginTimeMills = caseItemList.get(caseItemList.size() - 1).getRecordTime();
                replayActionCaseItemRepository.save(caseItemList);
            }
            replayActionItem.setLastRecordTime(beginTimeMills);
        }
        return size;
    }
}

package com.arextest.schedule.progress.impl;

import com.arextest.schedule.comparer.CompareConfigService;
import com.arextest.schedule.dao.mongodb.ReplayPlanActionRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanRepository;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.service.ReplayReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author jmo
 * @since 2021/10/11
 */
@Slf4j
@Component
final class UpdateResultProgressEventImpl implements ProgressEvent {
    @Resource
    private ReplayPlanActionRepository replayPlanActionRepository;
    @Resource
    private ReplayPlanRepository replayPlanRepository;
    @Resource
    private ReplayReportService replayReportService;
    @Resource
    private CompareConfigService compareConfigService;

    @Override
    public void onReplayPlanCreated(ReplayPlan replayPlan) {
        try {
            replayReportService.initReportInfo(replayPlan);
            compareConfigService.preload(replayPlan);
        } catch (Throwable throwable) {
            LOGGER.error("prepare load compare config error: {}, plan id:{}", throwable.getMessage(),
                    replayPlan.getId(), throwable);
        }
    }


    @Override
    public void onReplayPlanFinish(ReplayPlan replayPlan, ReplayStatusType reason) {
        replayPlan.setPlanFinishTime(new Date());
        String planId = replayPlan.getId();
        boolean result = replayPlanRepository.finish(planId);
        LOGGER.info("update the replay plan finished, plan id:{} , result: {}", planId, result);
        replayReportService.pushPlanStatus(planId, reason, null);
    }

    @Override
    public void onReplayPlanInterrupt(ReplayPlan replayPlan, ReplayStatusType reason) {
        replayPlan.setPlanFinishTime(new Date());
        String planId = replayPlan.getId();
        boolean result = replayPlanRepository.finish(planId);
        LOGGER.info("update the replay plan finished, plan id:{} , result: {}", planId, result);
        replayReportService.pushPlanStatus(planId, reason, replayPlan.getErrorMessage());
    }

    @Override
    public void onActionComparisonFinish(ReplayActionItem actionItem) {
        actionItem.setReplayFinishTime(new Date());
        updateReplayActionStatus(actionItem, ReplayStatusType.FINISHED, null);
    }

    @Override
    public void onActionBeforeSend(ReplayActionItem actionItem) {
        actionItem.setReplayBeginTime(new Date());
        updateReplayActionStatus(actionItem, ReplayStatusType.RUNNING, null);
    }

    private void updateReplayActionStatus(ReplayActionItem actionItem, ReplayStatusType replayStatusType, String errorMessage) {
        actionItem.setReplayStatus(replayStatusType.getValue());
        replayPlanActionRepository.update(actionItem);
        LOGGER.info("update the replay action send status: {}, action id:{}", replayStatusType, actionItem.getId());
        replayReportService.pushActionStatus(actionItem.getPlanId(),
                replayStatusType, actionItem.getId(), errorMessage);
    }

    @Override
    public void onActionAfterSend(ReplayActionItem actionItem) {

    }

    @Override
    public void onActionCaseLoaded(ReplayActionItem actionItem) {
        if (actionItem.isEmpty()) {
            LOGGER.info("loaded empty case , action id:{} , should skip it all", actionItem.getId());
            return;
        }
        replayPlanActionRepository.update(actionItem);
        LOGGER.info("update the replay action case count, action id:{} , size: {}", actionItem.getId(),
                actionItem.getReplayCaseCount());
    }

    @Override
    public void onActionInterrupted(ReplayActionItem actionItem) {
        final Date now = new Date();
        if (actionItem.getReplayBeginTime() == null) {
            actionItem.setReplayBeginTime(now);
        }
        actionItem.setReplayFinishTime(now);
        updateReplayActionStatus(actionItem, ReplayStatusType.FAIL_INTERRUPTED, actionItem.getErrorMessage());
    }

    public void onActionCancelled(ReplayActionItem actionItem) {
        final Date now = new Date();
        if (actionItem.getReplayBeginTime() == null) {
            actionItem.setReplayBeginTime(now);
        }
        actionItem.setReplayFinishTime(now);
        updateReplayActionStatus(actionItem, ReplayStatusType.CANCELLED, null);
    }
}
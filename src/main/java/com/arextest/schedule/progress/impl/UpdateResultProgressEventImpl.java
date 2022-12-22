package com.arextest.schedule.progress.impl;

import com.arextest.schedule.comparer.CompareConfigService;
import com.arextest.schedule.dao.mongodb.ReplayPlanActionRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanRepository;
import com.arextest.schedule.model.LogType;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.service.MetricService;
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
    @Resource
    private MetricService metricService;

    public static final long DEFAULT_COUNT = 1L;


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
        recordPlanExecutionTime(replayPlan);
    }

    @Override
    public void onReplayPlanInterrupt(ReplayPlan replayPlan, ReplayStatusType reason) {
        replayPlan.setPlanFinishTime(new Date());
        String planId = replayPlan.getId();
        boolean result = replayPlanRepository.finish(planId);
        LOGGER.info("update the replay plan finished, plan id:{} , result: {}", planId, result);
        metricService.recordCountEvent(LogType.PLAN_EXCEPTION_NUMBER.getValue(), replayPlan.getId(), replayPlan.getAppId(), DEFAULT_COUNT);
        replayReportService.pushPlanStatus(planId, reason, replayPlan.getErrorMessage());
        recordPlanExecutionTime(replayPlan);
    }

    private void recordPlanExecutionTime(ReplayPlan replayPlan) {
        Date planCreateTime = replayPlan.getPlanCreateTime();
        long planFinishMills = replayPlan.getPlanFinishTime() == null ? System.currentTimeMillis() : replayPlan.getPlanFinishTime().getTime();
        if (planCreateTime != null) {
            metricService.recordTimeEvent(LogType.PLAN_EXECUTION_TIME.getValue(), replayPlan.getId(), replayPlan.getAppId(), null,
                    planFinishMills - planCreateTime.getTime());
        } else {
            LOGGER.warn("record plan execution time fail, plan create time is null, plan id :{}", replayPlan.getId());
        }
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
        metricService.recordCountEvent(LogType.CASE_EXCEPTION_NUMBER.getValue(), actionItem.getPlanId(), actionItem.getAppId(),
                actionItem.getCaseItemList().size());
    }

    public void onActionCancelled(ReplayActionItem actionItem) {
        final Date now = new Date();
        if (actionItem.getReplayBeginTime() == null) {
            actionItem.setReplayBeginTime(now);
        }
        actionItem.setReplayFinishTime(now);
        updateReplayActionStatus(actionItem, ReplayStatusType.CANCELLED, null);
    }

    public void onActionCancelled(ReplayActionItem actionItem) {
        final Date now = new Date();
        if (actionItem.getReplayBeginTime() == null) {
            actionItem.setReplayBeginTime(now);
        }
        actionItem.setReplayFinishTime(now);
        updateReplayActionStatus(actionItem, ReplayStatusType.CANCELLED);
    }
}
package com.arextest.schedule.progress.impl;

import com.arextest.schedule.comparer.CompareConfigService;
import com.arextest.schedule.dao.mongodb.ReplayPlanActionRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanRepository;
import com.arextest.schedule.exceptions.ReRunPlanException;
import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.LogType;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.model.plan.PlanStageEnum;
import com.arextest.schedule.model.plan.ReplayPlanStageInfo;
import com.arextest.schedule.model.plan.StageBaseInfo;
import com.arextest.schedule.model.plan.StageStatusEnum;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.service.MetricService;
import com.arextest.schedule.service.ReplayReportService;
import com.arextest.schedule.utils.StageUtils;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;


import javax.annotation.Resource;
import java.util.Date;

/**
 * @author jmo
 * @since 2021/10/11
 */
@Slf4j
public class UpdateResultProgressEventImpl implements ProgressEvent {
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
            onReplayPlanStageUpdate(replayPlan, PlanStageEnum.INIT_REPORT, StageStatusEnum.ONGOING,
                System.currentTimeMillis(), null, null);
            boolean success = replayReportService.initReportInfo(replayPlan);
            StageStatusEnum stageStatusEnum = StageStatusEnum.success(success);
            onReplayPlanStageUpdate(replayPlan, PlanStageEnum.INIT_REPORT, stageStatusEnum,
                null, System.currentTimeMillis(), null);

            onReplayPlanStageUpdate(replayPlan, PlanStageEnum.LOADING_CONFIG, StageStatusEnum.ONGOING,
                System.currentTimeMillis(), null, null);
            compareConfigService.preload(replayPlan);
            onReplayPlanStageUpdate(replayPlan, PlanStageEnum.LOADING_CONFIG, StageStatusEnum.SUCCEEDED,
                null, System.currentTimeMillis(), null);
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
        replayPlanRepository.finish(planId);
        LOGGER.info("The plan was interrupted, plan id:{} ,appId: {} ", replayPlan.getId(), replayPlan.getAppId());
        metricService.recordCountEvent(LogType.PLAN_EXCEPTION_NUMBER.getValue(), replayPlan.getId(), replayPlan.getAppId(), DEFAULT_COUNT);
        replayReportService.pushPlanStatus(planId, reason, replayPlan.getErrorMessage());
        recordPlanExecutionTime(replayPlan);
    }

    @Override
    public void onReplayPlanTerminate(String replayId) {
        replayPlanRepository.finish(replayId);
        replayReportService.pushPlanStatus(replayId, ReplayStatusType.CANCELLED, null);
    }

    @Override
    public void onReplayPlanStageUpdate(ReplayPlan replayPlan, PlanStageEnum stageType, StageStatusEnum stageStatus,
                                        Long startTime, Long endTime, String msg) {
        StageBaseInfo stageBaseInfo;
        // if the plan is canceled, ignore the stage update
        if (replayPlan.getPlanStatus() != null && replayPlan.getPlanStatus().isCanceled()) {
            return;
        }
        if (stageType.isMainStage()) {
            stageBaseInfo = findStage(replayPlan.getReplayPlanStageList(), stageType);
        } else {
            ReplayPlanStageInfo parentStage = findParentStage(replayPlan.getReplayPlanStageList(), stageType);
            updateParentStage(parentStage, stageType, stageStatus, startTime, endTime, msg);

            stageBaseInfo = findStage(parentStage.getSubStageInfoList(), stageType);
        }
        if (stageBaseInfo != null) {
            updateStage(stageBaseInfo, stageType, stageStatus, startTime, endTime, msg);
        }
        replayPlan.setLastUpdateTime(System.currentTimeMillis());
    }

    @Override
    public void onReplayPlanReRun(ReplayPlan replayPlan) {
        replayReportService.pushPlanStatus(replayPlan.getId(), ReplayStatusType.RUNNING, null);
        addReRunStage(replayPlan.getReplayPlanStageList());
    }

    private StageBaseInfo findStage(List<ReplayPlanStageInfo> stageInfoList, PlanStageEnum stageType) {
        StageBaseInfo stageBaseInfo = null;
        for (StageBaseInfo stage : stageInfoList) {
            if (stageType.getCode() == stage.getStageType()) {
                stageBaseInfo = stage;
                break;
            }
        }
        return stageBaseInfo;
    }

    private ReplayPlanStageInfo findParentStage(List<ReplayPlanStageInfo> stageInfoList, PlanStageEnum stageType) {
        ReplayPlanStageInfo parentStage = null;
        PlanStageEnum parentStageEnum = PlanStageEnum.of(stageType.getParentStage());
        for (ReplayPlanStageInfo stage : stageInfoList) {
            if (parentStageEnum.getCode() == stage.getStageType()) {
                parentStage = stage;
                break;
            }
        }
        return parentStage;
    }

    private void updateStage(StageBaseInfo stageBaseInfo, PlanStageEnum stageType, StageStatusEnum stageStatus,
                                   Long startTime, Long endTime, String msg) {
        if (stageBaseInfo != null) {
            if (msg == null) {
                msg = String.format(StageUtils.MSG_FORMAT, stageType.name(), stageStatus);
            }
            stageBaseInfo.setStageType(stageType.getCode());
            stageBaseInfo.setStageName(stageType.name());
            if (stageStatus != null) {
                stageBaseInfo.setStageStatus(stageStatus.getCode());
            }
            stageBaseInfo.setMsg(msg);
            if (startTime != null) {
                stageBaseInfo.setStartTime(startTime);
            }
            if (endTime != null) {
                stageBaseInfo.setEndTime(endTime);
            }
        }
    }

    private void updateParentStage(StageBaseInfo parentStage, PlanStageEnum stageType, StageStatusEnum stageStatus,
                                   Long startTime, Long endTime, String msg) {
        PlanStageEnum parentStageEnum = PlanStageEnum.of(parentStage.getStageType());
        // when first subStage starts, start its parent stage
        boolean firstSubStageOnGoing =
            stageStatus == StageStatusEnum.ONGOING && parentStageEnum.getSubStageList().get(0) == stageType.getCode();
        // when last subStage successes, end its parent stage
        boolean lastSubStageSucceeded = stageStatus == StageStatusEnum.SUCCEEDED &&
            parentStageEnum.getSubStageList().get(parentStageEnum.getSubStageList().size() - 1) == stageType.getCode();
        // when any subStage fails, fail its parent stage
        if (firstSubStageOnGoing || lastSubStageSucceeded || stageStatus == StageStatusEnum.FAILED) {
            updateStage(parentStage, parentStageEnum, stageStatus, startTime, endTime, null);
        }
    }

    private void addReRunStage(List<ReplayPlanStageInfo> stageInfoList) {
        // reset stage after RUN&RERUN and add RERUN stage.
        int addIndex = 0;
        for (int index = 0; index < stageInfoList.size(); index ++) {
            if (stageInfoList.get(index).getStageType() == PlanStageEnum.RUN.getCode() ||
                stageInfoList.get(index).getStageType() == PlanStageEnum.RE_RUN.getCode()) {
                addIndex = index + 1;
            }
        }

        ReplayPlanStageInfo reRunStage = StageUtils.initEmptyStage(PlanStageEnum.RE_RUN);
        stageInfoList.add(addIndex, reRunStage);

        for (addIndex ++; addIndex < stageInfoList.size(); addIndex ++) {
            StageUtils.resetStageStatus(stageInfoList.get(addIndex));
        }

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
        actionItem.setReplayStatus(ReplayStatusType.CASE_LOADED.getValue());
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
                actionItem.getCaseItemList() == null ? 0 : actionItem.getCaseItemList().size());
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
package com.arextest.schedule.progress.impl;

import com.alibaba.fastjson2.util.DateUtils;
import com.arextest.common.cache.CacheProvider;
import com.arextest.schedule.dao.mongodb.ReplayPlanActionRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanRepository;
import com.arextest.schedule.model.LogType;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.model.plan.PlanStageEnum;
import com.arextest.schedule.model.plan.ReplayPlanStageInfo;
import com.arextest.schedule.model.plan.StageBaseInfo;
import com.arextest.schedule.model.plan.StageStatusEnum;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.service.MetricService;
import com.arextest.schedule.service.PlanProduceService;
import com.arextest.schedule.service.ReplayReportService;
import com.arextest.schedule.service.ReplayStorageService;
import com.arextest.schedule.utils.StageUtils;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jmo
 * @since 2021/10/11
 */
@Slf4j
public class UpdateResultProgressEventImpl implements ProgressEvent {

  public static final long DEFAULT_COUNT = 1L;
  @Resource
  private ReplayPlanActionRepository replayPlanActionRepository;
  @Resource
  private ReplayPlanRepository replayPlanRepository;
  @Resource
  private ReplayReportService replayReportService;
  @Resource
  private MetricService metricService;
  @Resource
  private CacheProvider redisCacheProvider;
  @Resource
  private ReplayStorageService replayStorageService;

  @Override
  public void onReplayPlanReRunException(ReplayPlan plan, Throwable t) {
    replayReportService.pushPlanStatus(plan.getId(), ReplayStatusType.FAIL_INTERRUPTED,
        t.getMessage(), true);
    redisCacheProvider.remove(PlanProduceService.buildPlanRunningRedisKey(plan.getId()));
  }

  @Override
  public void onReplayPlanCreated(ReplayPlan replayPlan) {
    try {
      onReplayPlanStageUpdate(replayPlan, PlanStageEnum.INIT_REPORT, StageStatusEnum.ONGOING,
          System.currentTimeMillis(), null, null);
      boolean success = replayReportService.initReportInfo(replayPlan);
      StageStatusEnum stageStatusEnum = StageStatusEnum.success(success);
      onReplayPlanStageUpdate(replayPlan, PlanStageEnum.INIT_REPORT, stageStatusEnum,
          null, System.currentTimeMillis(), null);
    } catch (Throwable throwable) {
      LOGGER.error("prepare load compare config error: {}, plan id:{}", throwable.getMessage(),
          replayPlan.getId(), throwable);
    }
  }

  @Override
  public void onCompareConfigBeforeLoading(ReplayPlan replayPlan) {
    onReplayPlanStageUpdate(replayPlan, PlanStageEnum.LOADING_CONFIG, StageStatusEnum.ONGOING,
        System.currentTimeMillis(), null, null);
  }

  @Override
  public void onCompareConfigLoaded(ReplayPlan replayPlan) {
    onReplayPlanStageUpdate(replayPlan, PlanStageEnum.LOADING_CONFIG, StageStatusEnum.SUCCEEDED,
        null, System.currentTimeMillis(), null);
  }


  @Override
  public void onReplayPlanFinish(ReplayPlan replayPlan, ReplayStatusType reason) {
    replayPlan.setPlanFinishTime(new Date());
    String planId = replayPlan.getId();
    boolean result = replayPlanRepository.finish(planId);
    LOGGER.info("update the replay plan finished, plan id:{} , result: {}", planId, result);
    replayReportService.pushPlanStatus(planId, reason, null, replayPlan.isReRun());
    recordPlanExecutionTime(replayPlan);
    redisCacheProvider.remove(PlanProduceService.buildPlanRunningRedisKey(replayPlan.getId()));
    replayStorageService.postProcessCompareResult(replayPlan.getId(), reason.getValue());
  }

  @Override
  public void onReplayPlanInterrupt(ReplayPlan replayPlan, ReplayStatusType reason) {
    replayPlan.setPlanFinishTime(new Date());
    String planId = replayPlan.getId();
    replayPlanRepository.finish(planId);
    LOGGER.info("The plan was interrupted, plan id:{} ,appId: {} ", replayPlan.getId(),
        replayPlan.getAppId());
    metricService.recordCountEvent(LogType.PLAN_EXCEPTION_NUMBER.getValue(), replayPlan.getId(),
        replayPlan.getAppId(), DEFAULT_COUNT);
    replayReportService.pushPlanStatus(planId, reason, replayPlan.getErrorMessage(),
        replayPlan.isReRun());
    recordPlanExecutionTime(replayPlan);
    redisCacheProvider.remove(PlanProduceService.buildPlanRunningRedisKey(replayPlan.getId()));
  }

  @Override
  public void onReplayPlanTerminate(String replayId) {
    replayPlanRepository.finish(replayId);
    replayReportService.pushPlanStatus(replayId, ReplayStatusType.CANCELLED, null, false);
    redisCacheProvider.remove(PlanProduceService.buildPlanRunningRedisKey(replayId));
  }

  @Override
  public void onReplayPlanStageUpdate(ReplayPlan replayPlan, PlanStageEnum stageType,
      StageStatusEnum stageStatus,
      Long startTime, Long endTime, String msg) {
    StageBaseInfo stageBaseInfo;
    // if the plan is canceled, ignore the stage update
    if (replayPlan.getPlanStatus() != null && replayPlan.getPlanStatus().isCanceled()) {
      return;
    }
    if (stageType.isMainStage()) {
      stageBaseInfo = findStage(replayPlan.getReplayPlanStageList(), stageType);
    } else {
      ReplayPlanStageInfo parentStage = findParentStage(replayPlan.getReplayPlanStageList(),
          stageType);
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
    replayReportService.pushPlanStatus(replayPlan.getId(), ReplayStatusType.RERUNNING, null,
        replayPlan.isReRun());
    redisCacheProvider.remove(PlanProduceService.buildStopPlanRedisKey(replayPlan.getId()));
    addReRunStage(replayPlan.getReplayPlanStageList());
    replayPlanRepository.updateStage(replayPlan);
  }

  private StageBaseInfo findStage(List<ReplayPlanStageInfo> stageInfoList,
      PlanStageEnum stageType) {
    StageBaseInfo stageBaseInfo = null;
    for (int i = stageInfoList.size() - 1; i >= 0; i--) {
      ReplayPlanStageInfo stage = stageInfoList.get(i);
      if (stageType.getCode() == stage.getStageType()) {
        stageBaseInfo = stage;
        break;
      }
    }
    return stageBaseInfo;
  }

  private ReplayPlanStageInfo findParentStage(List<ReplayPlanStageInfo> stageInfoList,
      PlanStageEnum stageType) {
    ReplayPlanStageInfo parentStage = null;
    PlanStageEnum parentStageEnum = PlanStageEnum.of(stageType.getParentStage());
    for (int i = stageInfoList.size() - 1; i >= 0; i--) {
      ReplayPlanStageInfo stage = stageInfoList.get(i);
      if (parentStageEnum.getCode() == stage.getStageType()) {
        parentStage = stage;
        break;
      }
    }
    return parentStage;
  }

  private void updateStage(StageBaseInfo stageBaseInfo, PlanStageEnum stageType,
      StageStatusEnum stageStatus,
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

      if (stageType == PlanStageEnum.RE_RUN) {
        String startTimeStr =
            stageBaseInfo.getStartTime() == null ? ""
                : DateUtils.format(new Date(stageBaseInfo.getStartTime()));
        String endTimeStr =
            stageBaseInfo.getEndTime() == null ? ""
                : DateUtils.format(new Date(stageBaseInfo.getEndTime()));
        stageBaseInfo.setMsg(String.format(StageUtils.RUNNING_FORMAT, startTimeStr, endTimeStr));
      }
    }
  }

  private void updateParentStage(StageBaseInfo parentStage, PlanStageEnum stageType,
      StageStatusEnum stageStatus,
      Long startTime, Long endTime, String msg) {
    PlanStageEnum parentStageEnum = PlanStageEnum.of(parentStage.getStageType());
    // when first subStage starts, start its parent stage
    boolean firstSubStageOnGoing =
        stageStatus == StageStatusEnum.ONGOING
            && parentStageEnum.getSubStageList().get(0) == stageType.getCode();
    // when last subStage successes, end its parent stage
    boolean lastSubStageSucceeded = stageStatus == StageStatusEnum.SUCCEEDED &&
        parentStageEnum.getSubStageList().get(parentStageEnum.getSubStageList().size() - 1)
            == stageType.getCode();
    // when any subStage fails, fail its parent stage
    if (firstSubStageOnGoing || lastSubStageSucceeded || stageStatus == StageStatusEnum.FAILED) {
      updateStage(parentStage, parentStageEnum, stageStatus, startTime, endTime, null);
    }
  }

  private void addReRunStage(List<ReplayPlanStageInfo> stageInfoList) {
    // reset stage after RUN&RERUN&CANCEL and add RERUN stage.
    int addIndex = 0;
    for (int index = 0; index < stageInfoList.size(); index++) {
      if (stageInfoList.get(index).getStageType() == PlanStageEnum.RUN.getCode() ||
          stageInfoList.get(index).getStageType() == PlanStageEnum.RE_RUN.getCode() ||
          stageInfoList.get(index).getStageType() == PlanStageEnum.CANCEL.getCode()) {
        addIndex = index + 1;
      }
    }

    ReplayPlanStageInfo reRunStage = StageUtils.initEmptyStage(PlanStageEnum.RE_RUN);
    stageInfoList.add(addIndex, reRunStage);

    for (addIndex++; addIndex < stageInfoList.size(); addIndex++) {
      StageUtils.resetStageStatus(stageInfoList.get(addIndex));
    }

  }

  private void recordPlanExecutionTime(ReplayPlan replayPlan) {
    Date planCreateTime = replayPlan.getPlanCreateTime();
    long planFinishMills = replayPlan.getPlanFinishTime() == null ? System.currentTimeMillis()
        : replayPlan.getPlanFinishTime().getTime();
    if (planCreateTime != null) {
      metricService.recordTimeEvent(LogType.PLAN_EXECUTION_TIME.getValue(), replayPlan.getId(),
          replayPlan.getAppId(), null,
          planFinishMills - planCreateTime.getTime());
    } else {
      LOGGER.warn("record plan execution time fail, plan create time is null, plan id :{}",
          replayPlan.getId());
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
    boolean isReRun = Optional.ofNullable(actionItem.getParent()).map(ReplayPlan::isReRun)
        .orElse(false);
    ReplayStatusType statusType = isReRun ? ReplayStatusType.RERUNNING : ReplayStatusType.RUNNING;
    updateReplayActionStatus(actionItem, statusType, null);
  }

  private void updateReplayActionStatus(ReplayActionItem actionItem,
      ReplayStatusType replayStatusType, String errorMessage) {
    actionItem.setReplayStatus(replayStatusType.getValue());
    boolean rerun = Optional.ofNullable(actionItem.getParent()).map(ReplayPlan::isReRun)
        .orElse(false);
    replayPlanActionRepository.update(actionItem);
    LOGGER.info("update the replay action send status: {}, action id:{}", replayStatusType,
        actionItem.getId());
    replayReportService.pushActionStatus(actionItem.getPlanId(),
        replayStatusType, actionItem.getId(), errorMessage, rerun);
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
    updateReplayActionStatus(actionItem, ReplayStatusType.FAIL_INTERRUPTED,
        actionItem.getErrorMessage());
    metricService.recordCountEvent(LogType.CASE_EXCEPTION_NUMBER.getValue(), actionItem.getPlanId(),
        actionItem.getAppId(),
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
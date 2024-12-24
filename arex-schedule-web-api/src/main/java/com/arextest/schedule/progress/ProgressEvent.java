package com.arextest.schedule.progress;

import com.arextest.schedule.exceptions.PlanRunningException;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.model.plan.BuildReplayPlanRequest;
import com.arextest.schedule.model.plan.PlanStageEnum;
import com.arextest.schedule.model.plan.StageStatusEnum;
import java.util.List;

/**
 * @author jmo
 * @since 2021/10/11
 */
public interface ProgressEvent {

  /**
   * @param request createPlanReq
   * @return bad response describing the reason blocking plan creation
   */
  default void onBeforePlanCreate(BuildReplayPlanRequest request) throws PlanRunningException {
  }

  default void onBeforePlanReRun(ReplayPlan replayPlan) throws PlanRunningException {
  }

  /**
   * call when create plan encounter logical or unchecked runtime exception
   *
   * @param request the request of create plan
   */
  default void onReplayPlanCreateException(BuildReplayPlanRequest request, Throwable t) {
  }

  default void onReplayPlanCreateException(BuildReplayPlanRequest request) {
  }

  default void onReplayPlanReRunException(ReplayPlan plan, Throwable t) {
  }

  default void onReplayPlanReRunException(ReplayPlan plan) {
  }

  void onReplayPlanCreated(ReplayPlan replayPlan);

  void onCompareConfigBeforeLoading(ReplayPlan replayPlan);

  void onCompareConfigLoaded(ReplayPlan replayPlan);


  default void onReplayPlanFinish(ReplayPlan replayPlan) {
    if (this.onBeforeReplayPlanFinish(replayPlan)) {
      this.onReplayPlanFinish(replayPlan, ReplayStatusType.FINISHED);
      this.onReplayPlanStageUpdate(replayPlan, PlanStageEnum.TASK_FINISH, StageStatusEnum.SUCCEEDED, System.currentTimeMillis(), null);
    } else {
      this.onReplayPlanAutoRerun(replayPlan);
    }
  }

  /**
   * @return: true-finish, false-rerun.
   */
  boolean onBeforeReplayPlanFinish(ReplayPlan replayPlan);

  void onReplayPlanAutoRerun(ReplayPlan replayPlan);

  void onReplayPlanFinish(ReplayPlan replayPlan, ReplayStatusType reason);

  void onReplayPlanInterrupt(ReplayPlan replayPlan, ReplayStatusType reason);

  void onReplayPlanTerminate(String planId, String reason);

  void onReplayPlanStageUpdate(ReplayPlan replayPlan, PlanStageEnum stageType,
      StageStatusEnum stageStatus, Long startTime, Long endTime, String msg);

  default void onReplayPlanStageUpdate(ReplayPlan replayPlan, PlanStageEnum stageType,
      StageStatusEnum stageStatus, Long startTime, Long endTime) {
    this.onReplayPlanStageUpdate(replayPlan, stageType, stageStatus, startTime, endTime, null);
  }

  void onReplayPlanReRun(ReplayPlan replayPlan);

  void onActionBeforeSend(ReplayActionItem actionItem);

  void onActionAfterSend(ReplayActionItem actionItem);

  void onActionCaseLoaded(ReplayActionItem actionItem);

  /**
   * After the replay of a single case ends
   * @param actionItem
   */
  default void onReplayCaseFinish(ReplayActionCaseItem actionItem){
  }

  default void onUpdateFailedCases(ReplayPlan replayPlan, List<ReplayActionCaseItem> caseItemList) {

  }

}
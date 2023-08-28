package com.arextest.schedule.progress;

import com.arextest.schedule.exceptions.PlanRunningException;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.model.plan.BuildReplayPlanRequest;
import com.arextest.schedule.model.plan.PlanStageEnum;
import com.arextest.schedule.model.plan.StageStatusEnum;

/**
 * @author jmo
 * @since 2021/10/11
 */
public interface ProgressEvent {
    /**
     * @param request createPlanReq
     * @return bad response describing the reason blocking plan creation
     */
    default void onBeforePlanCreate(BuildReplayPlanRequest request) throws PlanRunningException {}

    default void onBeforePlanReRun(ReplayPlan replayPlan) throws PlanRunningException {}

    /**
     * call when create plan encounter logical or unchecked runtime exception
     * @param request the request of create plan
     */
    default void onReplayPlanCreateException(BuildReplayPlanRequest request, Throwable t) {}
    default void onReplayPlanCreateException(BuildReplayPlanRequest request) {}

    default void onReplayPlanReRunException(ReplayPlan plan, Throwable t) {}
    default void onReplayPlanReRunException(ReplayPlan plan) {}

    void onReplayPlanCreated(ReplayPlan replayPlan);

    void onCompareConfigBeforeLoading(ReplayPlan replayPlan);
    void onCompareConfigLoaded(ReplayPlan replayPlan);


    default void onReplayPlanFinish(ReplayPlan replayPlan) {
        this.onReplayPlanFinish(replayPlan, ReplayStatusType.FINISHED);
    }

    void onReplayPlanFinish(ReplayPlan replayPlan, ReplayStatusType reason);

    void onReplayPlanInterrupt(ReplayPlan replayPlan, ReplayStatusType reason);

    void onReplayPlanTerminate(String replayId);

    void onReplayPlanStageUpdate(ReplayPlan replayPlan, PlanStageEnum stageType, StageStatusEnum stageStatus,
                                 Long startTime, Long endTime, String msg);

    void onReplayPlanReRun(ReplayPlan replayPlan);

    void onActionComparisonFinish(ReplayActionItem actionItem);

    void onActionBeforeSend(ReplayActionItem actionItem);

    void onActionAfterSend(ReplayActionItem actionItem);

    void onActionCaseLoaded(ReplayActionItem actionItem);

    void onActionInterrupted(ReplayActionItem actionItem);

    void onActionCancelled(ReplayActionItem actionItem);
}
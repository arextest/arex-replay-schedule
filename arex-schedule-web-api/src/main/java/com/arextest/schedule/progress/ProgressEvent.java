package com.arextest.schedule.progress;

import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.model.plan.BuildReplayPlanRequest;
import com.arextest.schedule.model.plan.PlanStageEnum;
import com.arextest.schedule.model.plan.ReplayPlanStageInfo;
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
    default void onBeforePlanCreate(BuildReplayPlanRequest request) {}

    /**
     * call when create plan encounter logical or unchecked runtime exception
     * @param request the request of create plan
     */
    default void onReplayPlanCreateException(BuildReplayPlanRequest request, Throwable t) {}
    default void onReplayPlanCreateException(BuildReplayPlanRequest request) {}

    void onReplayPlanCreated(ReplayPlan replayPlan);

    default void onReplayPlanFinish(ReplayPlan replayPlan) {
        this.onReplayPlanFinish(replayPlan, ReplayStatusType.FINISHED);
    }

    void onReplayPlanFinish(ReplayPlan replayPlan, ReplayStatusType reason);

    void onReplayPlanInterrupt(ReplayPlan replayPlan, ReplayStatusType reason);

    void onReplayPlanTerminate(String replayId);

    void onReplayPlanStageUpdate(ReplayPlan replayPlan, PlanStageEnum stageType, StageStatusEnum stageStatus,
                                 Long startTime, Long endTime, String msg);

    void onActionComparisonFinish(ReplayActionItem actionItem);

    void onActionBeforeSend(ReplayActionItem actionItem);

    void onActionAfterSend(ReplayActionItem actionItem);

    void onActionCaseLoaded(ReplayActionItem actionItem);

    void onActionInterrupted(ReplayActionItem actionItem);

    void onActionCancelled(ReplayActionItem actionItem);
}
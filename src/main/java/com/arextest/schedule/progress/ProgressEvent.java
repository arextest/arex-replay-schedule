package com.arextest.schedule.progress;

import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.model.plan.BuildReplayPlanRequest;

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
     * @param t null if encountered logical issue, throwable for all unchecked exceptions
     */
    default void onReplayPlanCreateException(BuildReplayPlanRequest request, Throwable t) {}

    void onReplayPlanCreated(ReplayPlan replayPlan);

    default void onReplayPlanFinish(ReplayPlan replayPlan) {
        this.onReplayPlanFinish(replayPlan, ReplayStatusType.FINISHED);
    }

    void onReplayPlanFinish(ReplayPlan replayPlan, ReplayStatusType reason);

    void onReplayPlanInterrupt(ReplayPlan replayPlan, ReplayStatusType reason);

    void onActionComparisonFinish(ReplayActionItem actionItem);

    void onActionBeforeSend(ReplayActionItem actionItem);

    void onActionAfterSend(ReplayActionItem actionItem);

    void onActionCaseLoaded(ReplayActionItem actionItem);

    void onActionInterrupted(ReplayActionItem actionItem);

    void onActionCancelled(ReplayActionItem actionItem);
}
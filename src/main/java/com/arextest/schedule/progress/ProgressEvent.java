package com.arextest.schedule.progress;

import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;

/**
 * @author jmo
 * @since 2021/10/11
 */
public interface ProgressEvent {
    void onReplayPlanCreated(ReplayPlan replayPlan);

    default void onReplayPlanFinish(ReplayPlan replayPlan) {
        this.onReplayPlanFinish(replayPlan, ReplayStatusType.FINISHED);
    }

    void onReplayPlanFinish(ReplayPlan replayPlan, ReplayStatusType reason);

    void onActionComparisonFinish(ReplayActionItem actionItem);

    void onActionBeforeSend(ReplayActionItem actionItem);

    void onActionAfterSend(ReplayActionItem actionItem);

    void onActionCaseLoaded(ReplayActionItem actionItem);

    void onActionInterrupted(ReplayActionItem actionItem);

    void onActionCancelled(ReplayActionItem actionItem);
}
package io.arex.replay.schedule.progress;

import io.arex.replay.schedule.model.ReplayActionItem;
import io.arex.replay.schedule.model.ReplayPlan;
import io.arex.replay.schedule.model.ReplayStatusType;

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
}

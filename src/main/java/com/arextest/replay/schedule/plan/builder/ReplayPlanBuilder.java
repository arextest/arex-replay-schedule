package com.arextest.replay.schedule.plan.builder;

import com.arextest.replay.schedule.model.ReplayActionItem;
import com.arextest.replay.schedule.model.plan.BuildReplayPlanRequest;
import com.arextest.replay.schedule.plan.PlanContext;

import java.util.List;

/**
 * @author jmo
 * @since 2021/9/18
 */
public interface ReplayPlanBuilder {
    boolean isSupported(BuildReplayPlanRequest request);

    BuildPlanValidateResult validate(BuildReplayPlanRequest request, PlanContext planContext);

    List<ReplayActionItem> buildReplayActionList(BuildReplayPlanRequest request, PlanContext planContext);

    void preprocess(List<ReplayActionItem> replayActionItemList, PlanContext planContext);

    int buildReplayCaseCount(List<ReplayActionItem> actionItemList);

}

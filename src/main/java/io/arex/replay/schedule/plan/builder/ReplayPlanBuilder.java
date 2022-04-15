package io.arex.replay.schedule.plan.builder;

import io.arex.replay.schedule.model.ReplayActionItem;
import io.arex.replay.schedule.model.plan.BuildReplayPlanRequest;
import io.arex.replay.schedule.plan.PlanContext;

import java.util.List;

/**
 * @author jmo
 * @since 2021/9/18
 */
public interface ReplayPlanBuilder {
    boolean isSupported(BuildReplayPlanRequest request);

    BuildPlanValidateResult validate(BuildReplayPlanRequest request, PlanContext planContext);

    List<ReplayActionItem> buildReplayActionList(BuildReplayPlanRequest request, PlanContext planContext);

    int buildReplayCaseCount(List<ReplayActionItem> actionItemList);

}

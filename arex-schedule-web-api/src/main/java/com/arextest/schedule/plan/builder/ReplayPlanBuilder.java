package com.arextest.schedule.plan.builder;

import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.plan.BuildReplayPlanRequest;
import com.arextest.schedule.plan.PlanContext;
import java.util.List;

/**
 * @author jmo
 * @since 2021/9/18
 */
public interface ReplayPlanBuilder {

  boolean isSupported(BuildReplayPlanRequest request);

  BuildPlanValidateResult validate(BuildReplayPlanRequest request, PlanContext planContext);

  List<ReplayActionItem> buildReplayActionList(BuildReplayPlanRequest request,
      PlanContext planContext);

  int buildReplayCaseCount(List<ReplayActionItem> actionItemList);

  void filterAppServiceDescriptors(BuildReplayPlanRequest request, PlanContext planContext);
}
package com.arextest.schedule.planexecution;

import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayPlan;

import java.util.List;

/**
 * Created by Qzmo on 2023/5/15
 */
public interface PlanExecutionContextProvider {
    List<PlanExecutionContext> buildContext(ReplayPlan plan);
    void onBeforeContextExecution(PlanExecutionContext currentContext, ReplayPlan plan);
    void onAfterContextExecution(PlanExecutionContext currentContext, ReplayPlan plan);
}

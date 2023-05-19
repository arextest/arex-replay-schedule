package com.arextest.schedule.planexecution;

import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayPlan;

import java.util.List;

/**
 * Created by Qzmo on 2023/5/15
 */
public interface PlanExecutionContextProvider<T> {
    List<PlanExecutionContext<T>> buildContext(ReplayPlan plan);
    void injectContextIntoCase(List<ReplayActionCaseItem> cases);
    void onBeforeContextExecution(PlanExecutionContext<T> currentContext, ReplayPlan plan);
    void onAfterContextExecution(PlanExecutionContext<T> currentContext, ReplayPlan plan);
}

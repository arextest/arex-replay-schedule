package com.arextest.schedule.planexecution;

import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayPlan;

import java.util.Collections;
import java.util.List;

/**
 * Created by Qzmo on 2023/5/15
 */
public class DefaultExecutionContextProvider implements PlanExecutionContextProvider {

    @Override
    public List<PlanExecutionContext> buildContext(ReplayPlan plan) {
        return Collections.singletonList(new PlanExecutionContext());
    }

    @Override
    public void onBeforeContextExecution(PlanExecutionContext currentContext, ReplayPlan plan) {

    }

    @Override
    public void onAfterContextExecution(PlanExecutionContext currentContext, ReplayPlan plan) {

    }
}

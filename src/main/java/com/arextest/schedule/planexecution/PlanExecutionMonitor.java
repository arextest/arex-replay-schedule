package com.arextest.schedule.planexecution;

import com.arextest.schedule.model.ReplayPlan;

/**
 * Created by Qzmo on 2023/6/16
 */
public interface PlanExecutionMonitor {
    void register(ReplayPlan plan);
    void deregister(ReplayPlan plan);
}

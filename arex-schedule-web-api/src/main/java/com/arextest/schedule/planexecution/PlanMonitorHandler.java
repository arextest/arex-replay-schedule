package com.arextest.schedule.planexecution;

import com.arextest.schedule.model.ReplayPlan;

/**
 * @author wildeslam.
 * @create 2023/7/31 15:44
 */
public interface PlanMonitorHandler {
    void handle(ReplayPlan plan);

    void finalize(ReplayPlan plan);
}

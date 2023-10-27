package com.arextest.schedule.planexecution;

import com.arextest.schedule.model.ReplayPlan;

/**
 * @author wildeslam.
 * @create 2023/7/31 15:44
 */
public interface PlanMonitorHandler {

  /**
   * Execute cyclically.
   */
  void handle(ReplayPlan plan);

  /**
   * Execute when deRegister.
   */
  void end(ReplayPlan plan);
}

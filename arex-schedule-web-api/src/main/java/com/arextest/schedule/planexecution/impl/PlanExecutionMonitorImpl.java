package com.arextest.schedule.planexecution.impl;

import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.planexecution.PlanExecutionMonitor;
import com.arextest.schedule.planexecution.PlanMonitorHandler;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created by Qzmo on 2023/6/16
 */
@Component
@Slf4j
public class PlanExecutionMonitorImpl implements PlanExecutionMonitor {

  public static final int SECOND_TO_REFRESH = 5;
  @Resource
  private ScheduledExecutorService monitorScheduler;
  @Resource
  private List<PlanMonitorHandler> planMonitorHandlerList;

  @Override
  public void monitorOne(ReplayPlan task) {
    if (task == null) {
      return;
    }
    LOGGER.info("Monitoring task {}", task.getId());
    for (PlanMonitorHandler handler : planMonitorHandlerList) {
      try {
        handler.handle(task);
      } catch (Throwable t) {
        LOGGER.error("Error handling plan:{}", task.getId(), t);
      }
    }
  }

  @Override
  public void register(ReplayPlan plan) {
    MonitorTask task = new MonitorTask(plan);
    ScheduledFuture<?> monitorFuture = monitorScheduler
        .scheduleAtFixedRate(task, 0, SECOND_TO_REFRESH, TimeUnit.SECONDS);
    plan.setMonitorFuture(monitorFuture);
  }

  @Override
  public void deregister(ReplayPlan plan) {
    plan.getMonitorFuture().cancel(false);
    LOGGER.info("deregister monitor task, planId: {}", plan.getId());

    for (PlanMonitorHandler handler : planMonitorHandlerList) {
      try {
        handler.end(plan);
      } catch (Throwable t) {
        LOGGER.error("Error ending plan:{}", plan.getId(), t);
      }
    }
  }

  /**
   * Monitor task for each Replay plan
   */
  private class MonitorTask implements Runnable {

    ReplayPlan replayPlan;

    MonitorTask(ReplayPlan replayPlan) {
      this.replayPlan = replayPlan;
    }

    @Override
    public void run() {
      MDCTracer.addPlanId(replayPlan.getId());
      try {
        monitorOne(replayPlan);
      } catch (Throwable t) {
        LOGGER.error("Error monitoring plan", t);
      } finally {
        MDCTracer.clear();
      }
    }
  }
}

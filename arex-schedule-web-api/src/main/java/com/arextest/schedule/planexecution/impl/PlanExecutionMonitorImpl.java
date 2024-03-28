package com.arextest.schedule.planexecution.impl;

import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.planexecution.PlanExecutionMonitor;
import com.arextest.schedule.planexecution.PlanMonitorHandler;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

/**
 * Created by Qzmo on 2023/6/16
 */
@Component
@Slf4j
public class PlanExecutionMonitorImpl implements PlanExecutionMonitor {

  public static final int DEFAULT_DELAY_SECOND = 5;
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
  public void register(ReplayPlan task) {
    LOGGER.info("register monitor task {}", task.getId());
    List<ScheduledFuture<?>> monitorFutures =
        Lists.newArrayListWithCapacity(planMonitorHandlerList.size());
    for (PlanMonitorHandler handler : planMonitorHandlerList) {
      try {
        ScheduledFuture<?> monitorFuture = monitorScheduler
            .scheduleAtFixedRate(new MonitorTask(task, handler), 0, handler.getDelayTime(),
                TimeUnit.SECONDS);
        monitorFutures.add(monitorFuture);
      } catch (Throwable t) {
        LOGGER.error("failed to register monitor task. plan:{}", task.getId(), t);
      }
    }
    task.setMonitorFutures(monitorFutures);
  }

  @Override
  public void deregister(ReplayPlan plan) {
    List<ScheduledFuture<?>> monitorFutures = plan.getMonitorFutures();
    if (CollectionUtils.isEmpty(monitorFutures)) {
      return;
    }
    LOGGER.info("deregister monitor task, planId: {}", plan.getId());
    monitorFutures.forEach(future -> future.cancel(false));

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
    PlanMonitorHandler handler;

    MonitorTask(ReplayPlan replayPlan, PlanMonitorHandler handler) {
      this.replayPlan = replayPlan;
      this.handler = handler;
    }

    @Override
    public void run() {
      MDCTracer.addPlanId(replayPlan.getId());
      try {
        handler.handle(replayPlan);
      } catch (Throwable t) {
        LOGGER.error("Error monitoring plan", t);
      } finally {
        MDCTracer.clear();
      }
    }
  }
}

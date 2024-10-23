package com.arextest.schedule.planexecution.impl;

import com.arextest.schedule.mdc.AbstractTracedRunnable;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.planexecution.PlanExecutionMonitor;
import com.arextest.schedule.planexecution.PlanMonitorHandler;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

/**
 * Created by Qzmo on 2023/6/16
 */
@Component
@Slf4j
public class PlanExecutionMonitorImpl implements PlanExecutionMonitor {

  @Resource
  private ScheduledExecutorService monitorScheduler;
  @Resource
  private List<PlanMonitorHandler> planMonitorHandlerList;

  @Override
  public void register(ReplayPlan plan) {
    if (CollectionUtils.isNotEmpty(plan.getMonitorFutures())) {
      LOGGER.info("duplicate register monitor task {}", plan.getId());
      return;
    }

    try {
      LOGGER.info("register monitor task {}", plan.getId());
      List<ScheduledFuture<?>> monitorFutures = Lists.newArrayListWithCapacity(planMonitorHandlerList.size());

      for (PlanMonitorHandler handler : planMonitorHandlerList) {
        ScheduledFuture<?> monitorFuture = monitorScheduler.scheduleAtFixedRate(new AbstractTracedRunnable() {
          @Override
          protected void doWithTracedRunning() {
            try {
              handler.handle(plan);
            } catch (Exception e) {
              LOGGER.error("failed to handle monitor task. plan:{}, handler: {}", plan.getId(),
                  handler.getClass().getName(), e);
            }
          }
        }, 0, handler.periodSeconds(), TimeUnit.SECONDS);
        monitorFutures.add(monitorFuture);
      }
      plan.setMonitorFutures(monitorFutures);
    } catch (Exception e) {
      LOGGER.error("failed to register monitor task. plan:{}", plan.getId(), e);
    }
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
      } catch (Exception e) {
        LOGGER.error("failed to end plan:{}, handler:{} ", plan.getId(), handler.getClass().getName(), e);
      }
    }
  }

}

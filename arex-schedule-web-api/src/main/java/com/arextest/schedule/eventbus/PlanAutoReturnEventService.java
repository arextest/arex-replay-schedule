package com.arextest.schedule.eventbus;

import com.arextest.schedule.exceptions.PlanRunningException;
import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.model.plan.ReRunReplayPlanRequest;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.service.PlanProduceService;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlanAutoReturnEventService implements InitializingBean {

  private static final String AUTO_OPERATOR = "Auto";

  @Resource
  private AsyncEventBus autoRerunAsyncEventBus;

  @Lazy
  @Resource
  private PlanProduceService planProduceService;

  @Lazy
  @Resource
  private ProgressEvent progressEvent;

  @Override
  public void afterPropertiesSet() throws Exception {
    autoRerunAsyncEventBus.register(this);
  }

  public void postRerunAsyncEvent(PlanAutoRerunEvent event) {
    autoRerunAsyncEventBus.post(event);
  }

  @Subscribe
  public void planAutoRerun(PlanAutoRerunEvent event) {
    ReRunReplayPlanRequest request = new ReRunReplayPlanRequest();
    request.setPlanId(event.getPlanId());
    request.setOperator(AUTO_OPERATOR);
    try {
      CommonResponse response = planProduceService.reRunPlan(request);
      if (response.getResult() != 1) {
        LOGGER.error("Auto rerun plan fail, planId: {}", event.getPlanId());
        finishPlan(event.getPlanId());
      }
    } catch (PlanRunningException e) {
      LOGGER.error("Auto rerun plan fail, planId: {}", event.getPlanId(), e);
      finishPlan(event.getPlanId());
    }
  }

  private void finishPlan(String planId) {
    ReplayPlan replayPlan = new ReplayPlan();
    replayPlan.setId(planId);
    progressEvent.onReplayPlanFinish(replayPlan, ReplayStatusType.FINISHED);
  }


}

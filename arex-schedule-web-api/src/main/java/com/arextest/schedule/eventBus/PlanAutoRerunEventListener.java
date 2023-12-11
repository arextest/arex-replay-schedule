package com.arextest.schedule.eventBus;

import com.arextest.schedule.exceptions.PlanRunningException;
import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.model.plan.ReRunReplayPlanRequest;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.service.PlanProduceService;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.Subscribe;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author wildeslam.
 * @create 2023/12/11 16:19
 */
@Slf4j
@Component
public class PlanAutoRerunEventListener {
  @Resource
  private PlanProduceService planProduceService;
  @Resource
  private ProgressEvent progressEvent;
  @Resource
  private AsyncEventBus asyncEventBus;

  @PostConstruct
  public void init() {
    asyncEventBus.register(this);
  }

  @Subscribe
  public void planAutoRerun(PlanAutoRerunEvent event) {
    ReRunReplayPlanRequest request = new ReRunReplayPlanRequest();
    request.setPlanId(event.getPlanId());
    request.setOperator("Auto");
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

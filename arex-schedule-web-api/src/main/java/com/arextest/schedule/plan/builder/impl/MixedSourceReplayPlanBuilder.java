package com.arextest.schedule.plan.builder.impl;

import com.arextest.schedule.model.plan.BuildReplayPlanRequest;
import com.arextest.schedule.model.plan.BuildReplayPlanType;
import org.springframework.stereotype.Component;

/**
 * @author: QizhengMo
 * @date: 2024/5/8
 */
@Component
public class MixedSourceReplayPlanBuilder extends AppIdSourceReplayPlanBuilder {

  @Override
  public boolean isSupported(BuildReplayPlanRequest request) {
    return request.getReplayPlanType() == BuildReplayPlanType.MIXED.getValue();
  }
}

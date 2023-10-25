package com.arextest.schedule.planexecution.impl;

import com.arextest.common.cache.CacheProvider;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.service.PlanProduceService;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author wildeslam.
 * @create 2023/7/26 16:03
 */
@Slf4j
@Component
public class RedisCancelMonitor {

  @Resource
  private CacheProvider redisCacheProvider;

  boolean isPlanCanceled(ReplayPlan plan) {
    return isPlanCanceled(plan.getId());
  }

  private boolean isPlanCanceled(String planId) {
    return isCancelled(PlanProduceService.buildStopPlanRedisKey(planId));
  }

  private boolean isCancelled(byte[] redisKey) {
    return redisCacheProvider.get(redisKey) != null;
  }
}

package com.arextest.schedule.utils;

import com.arextest.schedule.common.CommonConstant;
import java.nio.charset.StandardCharsets;

public class RedisKeyBuildUtils {


  // region Plan Status
  public static byte[] buildStopPlanRedisKey(String planId) {
    return (CommonConstant.STOP_PLAN_REDIS_KEY + planId).getBytes(StandardCharsets.UTF_8);
  }

  public static byte[] buildPlanRunningRedisKey(String planId) {
    return (String.format(CommonConstant.PLAN_RUNNING_KEY_FORMAT, planId)).getBytes(
        StandardCharsets.UTF_8);
  }
  // endregion

}

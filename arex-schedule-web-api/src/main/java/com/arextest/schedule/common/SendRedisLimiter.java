package com.arextest.schedule.common;

import com.arextest.common.cache.CacheProvider;
import com.arextest.schedule.model.ReplayPlan;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wildeslam.
 * @create 2023/12/1 15:38
 */
@Slf4j
public class SendRedisLimiter implements SendLimiter {

  public final static int SUCCESS_COUNT_TO_BALANCE_NO_ERROR = 20;
  public final static int CONTINUOUS_FAIL_TOTAL = 2 * SUCCESS_COUNT_TO_BALANCE_NO_ERROR;
  public final static double ERROR_BREAK_RATE = 0.1;

  private static final String REPLAY_PLAN_KEY_FORMAT = "replayPlan_%s_%s";
  private static final String CONTINUOUS_FAIL_COUNTER = "continuousFailCounter";
  private static final String FAIL_COUNTER = "failCounter";
  private static final long ONE_HOUR_SECONDS = TimeUnit.HOURS.toSeconds(1);

  private final ReplayPlan replayPlan;
  private final CacheProvider redisCacheProvider;

  public SendRedisLimiter(ReplayPlan replayPlan, CacheProvider redisCacheProvider) {
    this.replayPlan = replayPlan;
    this.redisCacheProvider = redisCacheProvider;
  }

  @Override
  public boolean failBreak() {
    int totalTasks = replayPlan.getCaseTotalCount();
    int continuousFailCounter = continuousError();
    int failCounter = totalError();
    return continuousFailCounter > CONTINUOUS_FAIL_TOTAL
        || (totalTasks > 0 && ((double) failCounter / totalTasks) > ERROR_BREAK_RATE);
  }

  @Override
  public void acquire() {

  }

  @Override
  public void release(boolean success) {
    byte[] continuousFailCounterKey = buildReplayPlanKeyWithKeyword(replayPlan.getId(),
        CONTINUOUS_FAIL_COUNTER);
    if (success) {
      long continuousFailCounter = byteArrayToInt(redisCacheProvider.get(continuousFailCounterKey));
      if (continuousFailCounter < CONTINUOUS_FAIL_TOTAL) {
        // reset to zero else pin down
        redisCacheProvider.put(continuousFailCounterKey, ONE_HOUR_SECONDS,
            String.valueOf(0).getBytes(StandardCharsets.UTF_8));
      }
      return;
    }
    redisCacheProvider.incrValue(continuousFailCounterKey);
    redisCacheProvider.incrValue(buildReplayPlanKeyWithKeyword(replayPlan.getId(), FAIL_COUNTER));
  }

  @Override
  public void batchRelease(boolean success, int size) {

  }

  @Override
  public int totalError() {
    return byteArrayToInt(
        redisCacheProvider.get(buildReplayPlanKeyWithKeyword(replayPlan.getId(), FAIL_COUNTER)));
  }

  @Override
  public int continuousError() {
    return byteArrayToInt(redisCacheProvider.get(
        buildReplayPlanKeyWithKeyword(replayPlan.getId(), CONTINUOUS_FAIL_COUNTER)));
  }

  @Override
  public void reset() {
    byte[] continuousFailCounterKey = buildReplayPlanKeyWithKeyword(replayPlan.getId(),
        CONTINUOUS_FAIL_COUNTER);
    redisCacheProvider.put(continuousFailCounterKey, ONE_HOUR_SECONDS,
        String.valueOf(0).getBytes(StandardCharsets.UTF_8));
    byte[] failCounterKey = buildReplayPlanKeyWithKeyword(replayPlan.getId(), FAIL_COUNTER);
    redisCacheProvider.put(failCounterKey, ONE_HOUR_SECONDS,
        String.valueOf(0).getBytes(StandardCharsets.UTF_8));
  }


  private int byteArrayToInt(byte[] bytes) {
    if (bytes.length != Integer.BYTES) {
      return 0;
    }
    return ByteBuffer.wrap(bytes).getInt();
  }

  private byte[] buildReplayPlanKeyWithKeyword(String planId, String keyword) {
    return (String.format(REPLAY_PLAN_KEY_FORMAT, planId, keyword)).getBytes(
        StandardCharsets.UTF_8);
  }

}

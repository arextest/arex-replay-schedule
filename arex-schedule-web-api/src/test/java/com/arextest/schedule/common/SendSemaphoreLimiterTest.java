package com.arextest.schedule.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Created by Qzmo on 2023/5/29
 */
public class SendSemaphoreLimiterTest {

  @Test
  public void invalidMax() {
    SendSemaphoreLimiter testInvalidMax = new SendSemaphoreLimiter(null, 1);
    assertTrue(testInvalidMax.getPermits() > 0);
  }

  @Test
  public void verySmallMax() {
    for (int i = 0; i < 10; i++) {
      SendSemaphoreLimiter small = new SendSemaphoreLimiter(i, 1);
      assertTrue(small.getPermits() > 0);
    }
  }

  @Test
  @Timeout(5)
  public void veryBigMax() {
    SendSemaphoreLimiter big = new SendSemaphoreLimiter(Integer.MAX_VALUE, 1);
    assertTrue(big.getPermits() != 0);

    for (int i = 0; i < 1000; i++) {
      big.acquire();
    }
  }

  @Test
  public void stepBack() {
    SendSemaphoreLimiter test = new SendSemaphoreLimiter(null, 1);
    for (int i = 0; i < 6; i++) {
      test.acquire();
      test.release(false);
    }
    assertEquals(1, test.getPermits());
  }

  @Test
  public void stepForward() {
    SendSemaphoreLimiter test = new SendSemaphoreLimiter(null, 1);
    for (int i = 0; i < SendSemaphoreLimiter.SUCCESS_COUNT_TO_BALANCE_NO_ERROR
        * SendSemaphoreLimiter.QPS_STEP_RATIO.length; i++) {
      test.acquire();
      test.release(true);
    }
    assertEquals(SendSemaphoreLimiter.DEFAULT_MAX_RATE, test.getPermits());
  }

  @Test
  public void breakByLimit() {
    SendSemaphoreLimiter test = new SendSemaphoreLimiter(null, 1);
    test.setTotalTasks(1000);
    test.batchRelease(false, (int) (1000 * SendSemaphoreLimiter.ERROR_BREAK_RATE));
    assertTrue(test.failBreak());
  }

  @Test
  public void breakByContinuousFail() {
    SendSemaphoreLimiter test = new SendSemaphoreLimiter(null, 1);
    test.setTotalTasks(1000);
    test.batchRelease(false, SendSemaphoreLimiter.CONTINUOUS_FAIL_TOTAL + 1);
    assertTrue(test.failBreak());
  }

  @Test
  public void batchSuccess() {
    SendSemaphoreLimiter test = new SendSemaphoreLimiter(null, 1);
    test.batchRelease(true, SendSemaphoreLimiter.SUCCESS_COUNT_TO_BALANCE_NO_ERROR + 1);
    test.acquire();
    assertEquals(SendSemaphoreLimiter.QPS_STEP_RATIO[1] * SendSemaphoreLimiter.DEFAULT_MAX_RATE,
        test.getPermits());
  }
}
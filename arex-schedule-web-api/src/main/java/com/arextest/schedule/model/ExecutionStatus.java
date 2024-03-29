package com.arextest.schedule.model;

import com.arextest.schedule.common.SendSemaphoreLimiter;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Builder;
import lombok.Getter;

/**
 * Created by Qzmo on 2023/5/15
 */
@Builder
public class ExecutionStatus {

  private AtomicReference<Boolean> canceled;
  @Getter
  private SendSemaphoreLimiter limiter;

  public static ExecutionStatus buildNormal(SendSemaphoreLimiter limiter) {
    return ExecutionStatus.builder()
        .canceled(new AtomicReference<>(false))
        .limiter(limiter)
        .build();
  }

  public boolean isNormal() {
    return !canceled.get() && !isInterrupted();
  }

  public boolean isAbnormal() {
    return canceled.get() || isInterrupted();
  }

  public boolean isInterrupted() {
    return this.limiter.failBreak();
  }

  public boolean isCanceled() {
    return this.canceled.get();
  }

  public void setCanceled(boolean newVal) {
    this.canceled.getAndUpdate((old) -> old || newVal);
  }
}

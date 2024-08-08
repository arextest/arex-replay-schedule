package com.arextest.schedule.model;

import com.arextest.schedule.common.RateLimiterFactory;
import com.arextest.schedule.common.SendSemaphoreLimiter;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Created by Qzmo on 2023/5/15
 */
@Builder
public class ExecutionStatus {

  private AtomicReference<Boolean> canceled;
  @Getter
  private Collection<SendSemaphoreLimiter> sendSemaphoreLimiterList;

  public static ExecutionStatus buildNormal(Collection<SendSemaphoreLimiter> sendSemaphoreLimiterList) {
    return ExecutionStatus.builder()
        .canceled(new AtomicReference<>(false))
        .sendSemaphoreLimiterList(sendSemaphoreLimiterList)
        .build();
  }

  public boolean isNormal() {
    return !canceled.get() && !isInterrupted();
  }

  public boolean isAbnormal() {
    return canceled.get() || isInterrupted();
  }

  public boolean isInterrupted() {
    if (CollectionUtils.isEmpty(sendSemaphoreLimiterList)) {
      return false;
    }
    return sendSemaphoreLimiterList.stream().allMatch(SendSemaphoreLimiter::failBreak);
  }

  public boolean isCanceled() {
    return this.canceled.get();
  }

  public void setCanceled(boolean newVal) {
    this.canceled.getAndUpdate((old) -> old || newVal);
  }
}

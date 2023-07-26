package com.arextest.schedule.model;

import com.arextest.schedule.common.SendSemaphoreLimiter;
import lombok.Builder;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Qzmo on 2023/5/15
 */
@Builder
public class ExecutionStatus {
    private AtomicReference<Boolean> canceled;
    @Getter
    private SendSemaphoreLimiter limiter;

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

    public static ExecutionStatus buildNormal(SendSemaphoreLimiter limiter) {
        return ExecutionStatus.builder()
                .canceled(new AtomicReference<>(false))
                .limiter(limiter)
                .build();
    }
}

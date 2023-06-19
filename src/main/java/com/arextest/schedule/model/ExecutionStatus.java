package com.arextest.schedule.model;

import lombok.Builder;
import lombok.Data;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Qzmo on 2023/5/15
 */
@Builder
public class ExecutionStatus {
    private AtomicReference<Boolean> canceled;
    private AtomicReference<Boolean> interrupted;

    public boolean isNormal() {
        return !canceled.get() && !interrupted.get();
    }
    public boolean isAbnormal() {
        return canceled.get() || interrupted.get();
    }

    public boolean isInterrupted() {
        return this.interrupted.get();
    }

    public void setInterrupted(boolean newVal) {
        this.interrupted.getAndUpdate((old) -> old || newVal);
    }

    public boolean isCanceled() {
        return this.canceled.get();
    }

    public void setCanceled(boolean newVal) {
        this.canceled.getAndUpdate((old) -> old || newVal);
    }

    public static ExecutionStatus buildNormal() {
        return ExecutionStatus.builder()
                .canceled(new AtomicReference<>(false))
                .interrupted(new AtomicReference<>(false))
                .build();
    }
}

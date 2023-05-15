package com.arextest.schedule.model;

import lombok.Builder;
import lombok.Data;

/**
 * Created by Qzmo on 2023/5/15
 */
@Data
@Builder
public class ExecutionStatus {
    private boolean canceled;
    private boolean interrupted;

    public boolean isNormal() {
        return !canceled && !interrupted;
    }

    public static ExecutionStatus buildNormal() {
        return ExecutionStatus.builder().build();
    }
}

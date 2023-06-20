package com.arextest.schedule.spi.model;

import lombok.Data;

import java.util.Map;

@Data
public class ReplayInvokeResult {
    /**
     * invoke result.
     */
    private Object result;

    /**
     * traceId to match record and replay.
     */
    private String replayId;

    /**
     * if invoke failed.
     */
    private String errorMsg;

    /**
     * if invoke failed.
     */
    private Exception exception;
}

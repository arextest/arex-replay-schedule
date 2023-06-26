package com.arextest.schedule.extension.model;

import lombok.Data;

import java.util.Map;

@Data
public class ReplayInvokeResult {
    /**
     * invoke result.
     */
    private Object result;

    private Map<String, String> responseProperties;

    /**
     * if invoke failed.
     */
    private String errorMsg;

    /**
     * if invoke failed.
     */
    private Exception exception;
}

package com.arextest.schedule;

import lombok.Data;

import java.util.Map;

@Data
public class ReplayInvokeResult {
    private Object result;

    private Map<String, String> attachments;

    private String errorMsg;

    private Exception exception;
}

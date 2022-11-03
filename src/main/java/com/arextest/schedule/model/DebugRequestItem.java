package com.arextest.schedule.model;

import com.arextest.schedule.sender.SenderParameters;
import lombok.Data;

import java.util.Map;

/**
 * @author: miaolu
 * @create: 2021-12-08
 **/
@Data
public class DebugRequestItem implements SenderParameters {
    private int requestType;
    private String appId;
    private String url;
    private String operation;
    private String format;
    private String message;
    private String subEnv;
    private String consumeGroup;
    private Map<String, String> headers;
}
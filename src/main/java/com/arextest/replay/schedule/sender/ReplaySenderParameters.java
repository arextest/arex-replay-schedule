package com.arextest.replay.schedule.sender;

import lombok.Data;

import java.util.Map;

/**
 * @author jmo
 * @since 2021/9/16
 **/
@Data
public class ReplaySenderParameters implements SenderParameters {
    private String appId;
    private String url;
    private String operation;
    private String format;
    private String message;
    private String subEnv;
    private String consumeGroup;
    private Map<String, String> headers;
    private String recordId;
    private String method;
}

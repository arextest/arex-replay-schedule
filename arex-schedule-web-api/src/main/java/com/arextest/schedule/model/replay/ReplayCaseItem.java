package com.arextest.schedule.model.replay;

import lombok.Data;

/**
 * @since 2023/11/17
 */
@Data
public class ReplayCaseItem {
    private String id;
    private boolean entryPoint;
    private String operationName;
    private String request;
    private String response;
    private long createTime;
    private String compareKey;
}


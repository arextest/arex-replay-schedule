package com.arextest.schedule.model.bizlog;

import lombok.Data;

import java.util.Optional;

/**
 * Created by Qzmo on 2023/6/8
 */
@Data
public class QueryReplayBizLogsRequest {
    private String planId;
    private ReplayBizLogQueryCondition condition;
}

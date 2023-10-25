package com.arextest.schedule.model.bizlog;

import lombok.Data;

/**
 * Created by Qzmo on 2023/6/8
 */
@Data
public class QueryReplayBizLogsRequest {

  private String planId;
  private ReplayBizLogQueryCondition condition;
}

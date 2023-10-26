package com.arextest.schedule.model.plan;

import lombok.Data;

/**
 * @author wildeslam.
 * @create 2023/8/10 20:03
 */
@Data
public class ReRunReplayPlanRequest {

  private String planId;
  private String operator;
}

package com.arextest.schedule.model.plan;

import java.util.List;
import javax.validation.constraints.Max;
import lombok.Data;

/**
 * @author wildeslam.
 * @create 2023/11/22 16:56
 */
@Data
public class QueryReplaySenderParametersRequest {
  private String planId;
  /**
   * @see BuildReplayPlanType
   */
  private int replayPlanType;
  @Max(100)
  private List<String> caseIds;

}

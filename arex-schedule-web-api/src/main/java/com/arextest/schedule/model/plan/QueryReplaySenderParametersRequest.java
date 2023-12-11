package com.arextest.schedule.model.plan;

import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Size;
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
  @Size(max = 100, min = 1)
  private List<String> caseIds;

}

package com.arextest.schedule.model.plan;

import jakarta.validation.constraints.Size;
import java.util.List;
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

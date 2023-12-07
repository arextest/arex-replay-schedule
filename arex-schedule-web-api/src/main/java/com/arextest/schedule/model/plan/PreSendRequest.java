package com.arextest.schedule.model.plan;

import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author wildeslam.
 * @create 2023/11/27 11:00
 */
@Data
public class PreSendRequest {
  @NotNull
  private String planId;
  @NotNull
  private String caseId;
  @NotNull
  private int replayPlanType;
  @NotNull
  private String recordId;

}

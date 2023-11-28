package com.arextest.schedule.model.plan;

import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author wildeslam.
 * @create 2023/11/27 11:12
 */
@Data
public class PostSendRequest {
  @NotNull
  private String caseId;
  @NotNull
  private String replayId;
  @NotNull
  private Integer sendStatusType;
  private String errorMsg;
}

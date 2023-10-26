package com.arextest.schedule.model.plan;

import lombok.Data;

/**
 * Created by Qzmo on 2023/6/29
 */
@Data
public class BuildReplayPlanResponse {

  private int reasonCode;
  private String replayPlanId;

  public BuildReplayPlanResponse(BuildReplayFailReasonEnum reason) {
    this.reasonCode = reason.getCode();
  }

  public BuildReplayPlanResponse(int errorCode) {
    this.reasonCode = errorCode;
  }

  public BuildReplayPlanResponse(String replayPlanId) {
    this.reasonCode = BuildReplayFailReasonEnum.NORMAL.getCode();
    this.replayPlanId = replayPlanId;
  }
}

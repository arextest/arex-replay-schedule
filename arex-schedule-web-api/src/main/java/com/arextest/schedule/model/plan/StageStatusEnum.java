package com.arextest.schedule.model.plan;

import lombok.Getter;

/**
 * @author wildeslam.
 * @create 2023/7/24 17:25
 */
public enum StageStatusEnum {
  UNKNOWN(0),
  PENDING(1),
  ONGOING(2),
  SUCCEEDED(3),
  FAILED(4);


  @Getter
  private final int code;

  StageStatusEnum(int code) {
    this.code = code;
  }

  public static StageStatusEnum success(boolean success) {
    return success ? SUCCEEDED : FAILED;
  }
}

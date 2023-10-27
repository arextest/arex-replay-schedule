package com.arextest.schedule.model.bizlog;

import lombok.Getter;

/**
 * Created by qzmo on 2023/5/31.
 */
public enum BizLogLevel {
  INFO(0),
  WARN(1),
  ERROR(2),
  DEBUG(3),
  ;

  @Getter
  private final int val;

  BizLogLevel(int val) {
    this.val = val;
  }
}

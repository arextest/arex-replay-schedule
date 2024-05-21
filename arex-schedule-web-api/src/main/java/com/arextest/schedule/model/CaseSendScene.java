package com.arextest.schedule.model;

public enum CaseSendScene {
  /**
   * normal cases
   */
  NORMAL,
  /**
   * extra cases like de-noise and config recover
   */
  EXTRA,

  /**
   * normal cases in mixed replay
   */
  MIXED_NORMAL,
  ;
}

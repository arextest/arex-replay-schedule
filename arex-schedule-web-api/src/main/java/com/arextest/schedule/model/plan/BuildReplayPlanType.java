package com.arextest.schedule.model.plan;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * @author jmo
 * @since 2021/9/18
 */
public enum BuildReplayPlanType {
  /**
   * app dimension replay
   */
  BY_APP_ID(0),
  /**
   * interface dimension replay
   */
  BY_OPERATION_OF_APP_ID(1),
  /**
   * case replay in the pinned table
   */
  BY_PINNED_CASE(2),

  /**
   * case replay in the rolling table
   */
  BY_ROLLING_CASE(3),

  /**
   * App Rolling + Auto pined
   */
  MIXED(4);

  private static final Map<Integer, BuildReplayPlanType> BY_VALUE_MAP = new HashMap<>();

  static {
    for (BuildReplayPlanType type : values()) {
      BY_VALUE_MAP.put(type.getValue(), type);
    }
  }

  @Getter
  final int value;

  BuildReplayPlanType(int value) {
    this.value = value;
  }

  public static BuildReplayPlanType findByValue(int value) {
    return BY_VALUE_MAP.get(value);
  }
}
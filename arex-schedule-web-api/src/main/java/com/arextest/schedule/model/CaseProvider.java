package com.arextest.schedule.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CaseProvider {
  ROLLING(0, "Rolling"),
  PINNED(1, "Pinned"),
  AUTO_PINED(2, "AutoPinned");

  private final int code;
  private final String name;
}

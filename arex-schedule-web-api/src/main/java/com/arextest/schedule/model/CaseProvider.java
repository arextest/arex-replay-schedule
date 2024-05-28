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

  public static CaseProvider fromCode(int code) {
    for (CaseProvider provider : values()) {
      if (provider.code == code) {
        return provider;
      }
    }
    return null;
  }
}

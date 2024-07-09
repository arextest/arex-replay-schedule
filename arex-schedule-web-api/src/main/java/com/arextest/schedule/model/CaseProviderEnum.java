package com.arextest.schedule.model;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CaseProviderEnum {
  ROLLING(0, "Rolling"),
  PINNED(1, "Pinned"),
  AUTO_PINNED(2, "AutoPinned");

  private final int code;
  private final String name;
  private static final Map<Integer, CaseProviderEnum> codeToProviderMap = new HashMap<>();

  static {
    for (CaseProviderEnum provider : values()) {
      codeToProviderMap.put(provider.code, provider);
    }
  }

  public static CaseProviderEnum fromCode(int code) {
    return codeToProviderMap.get(code);
  }
}

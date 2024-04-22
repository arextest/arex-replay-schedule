package com.arextest.schedule.service;

import org.springframework.beans.factory.annotation.Value;

public class DefaultConfigProviderImpl implements ConfigProvider {

  @Value("${arex.schedule.compare.delay.seconds:60}")
  private int delaySeconds;
  @Value("${arex.schedule.case.source.to.offset.millis:0}")
  private long caseSourceToOffsetMillis;

  /**
   * Get the time of delay comparison. Default 60s
   *
   * @param appId
   * @return
   */
  public int getCompareDelaySeconds(String appId) {
    return delaySeconds;
  }

  /**
   * Get the time of case source to offset millis. Default 0 millis
   * @return
   */
  public long getCaseSourceToOffsetMillis() {
    return caseSourceToOffsetMillis;
  }
}
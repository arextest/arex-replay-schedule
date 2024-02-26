package com.arextest.schedule.service;

import org.springframework.beans.factory.annotation.Value;

public class DefaultConfigProviderImpl implements ConfigProvider {

  @Value("${arex.schedule.compare.delay.seconds:60}")
  private int delaySeconds;

  /**
   * Get the time of delay comparison. Default 60s
   *
   * @param appId
   * @return
   */
  public int getCompareDelaySeconds(String appId) {
    return delaySeconds;
  }
}
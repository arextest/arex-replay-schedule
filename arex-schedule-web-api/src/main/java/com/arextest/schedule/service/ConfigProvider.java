package com.arextest.schedule.service;

/**
 * @author xinyuan_wang.
 * @create 2024/2/19
 * Get properties data.
 */
public interface ConfigProvider {

  /**
   * Get the time of delay comparison
   * @param appId
   * @return
   */
  int getCompareDelaySeconds(String appId);

}

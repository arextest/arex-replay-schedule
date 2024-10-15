package com.arextest.schedule.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * the rate limiter factory
 * @author thji
 * @date 2024/8/27
 * @since 1.0.0
 */
@AllArgsConstructor
public class RateLimiterFactory {

  private final int singleCaseTasks;
  private final double errorBreakRatio;
  private final int continuousFailThreshold;
  private final int replaySendMaxQps;

  private Map<String, SendSemaphoreLimiter> rateLimiterMap = new HashMap<>();

  public RateLimiterFactory(int singleCaseTasks, double errorBreakRatio,
      int continuousFailThreshold, int replaySendMaxQps) {
    this.singleCaseTasks = singleCaseTasks;
    this.errorBreakRatio = errorBreakRatio;
    this.continuousFailThreshold = continuousFailThreshold;
    this.replaySendMaxQps = replaySendMaxQps;
  }

  public SendSemaphoreLimiter getRateLimiter(String host) {
    if (StringUtils.isEmpty(host)) {
      return null;
    }
    return rateLimiterMap.computeIfAbsent(host, k -> {
          SendSemaphoreLimiter sendSemaphoreLimiter = new SendSemaphoreLimiter(
              replaySendMaxQps, 1);
          sendSemaphoreLimiter.setTotalTasks(singleCaseTasks);
          sendSemaphoreLimiter.setErrorBreakRatio(errorBreakRatio);
          sendSemaphoreLimiter.setContinuousFailThreshold(continuousFailThreshold);
          sendSemaphoreLimiter.setHost(host);
          return sendSemaphoreLimiter;
        }
    );
  }

  public Collection<SendSemaphoreLimiter> getAll() {
    return Collections.unmodifiableCollection(rateLimiterMap.values());
  }

}

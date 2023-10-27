package com.arextest.schedule.service;

import com.arextest.schedule.comparer.CategoryComparisonHolder;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.sender.ReplaySendResult;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Created by xinyuan_wang on 2023/4/19 record log information
 */
@Slf4j
public class MetricService {

  private final List<MetricListener> metricListeners;

  public MetricService(List<MetricListener> metricListeners) {
    this.metricListeners = metricListeners;
  }

  /**
   * record request time
   */
  public void recordTimeEvent(String logType, String planId, String appId, String request,
      long timeUsed) {
    if (CollectionUtils.isEmpty(this.metricListeners)) {
      return;
    }
    for (MetricListener listener : this.metricListeners) {
      listener.recordTimeAction(logType, planId, appId, request, timeUsed);
    }
  }

  /**
   * logging count
   */
  public void recordCountEvent(String logType, String planId, String appId, long count) {
    if (CollectionUtils.isEmpty(this.metricListeners)) {
      return;
    }
    for (MetricListener listener : this.metricListeners) {
      listener.recordCountAction(logType, planId, appId, count);
    }
  }

  /**
   * record send log info and invoke time
   */
  public void recordSendLogEvent(String logType, ReplaySendResult targetSendResult,
      ReplayActionCaseItem caseItem,
      long timeUsed) {
    if (CollectionUtils.isEmpty(this.metricListeners)) {
      return;
    }
    for (MetricListener listener : this.metricListeners) {
      listener.recordSendLogAction(logType, targetSendResult, caseItem, timeUsed);
    }
  }

  /**
   * get log message id from url and headers
   */
  public String generateMessageIdEvent(Map<String, String> headers, String url) {
    if (CollectionUtils.isEmpty(this.metricListeners)) {
      return null;
    }
    return this.metricListeners.get(0).generateMessageId(headers, url);
  }

  /**
   * todo record the QMessage replay log, which will be optimized for removal later.
   */
  public void recordTraceIdEvent(ReplayActionCaseItem caseItem,
      List<CategoryComparisonHolder> replayResult) {
    if (CollectionUtils.isEmpty(this.metricListeners)) {
      return;
    }
    for (MetricListener listener : this.metricListeners) {
      listener.recordTraceIdAction(caseItem, replayResult);
    }
  }
}

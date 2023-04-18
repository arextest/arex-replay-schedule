package com.arextest.schedule.service;


import com.arextest.schedule.comparer.CategoryComparisonHolder;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.sender.ReplaySendResult;

import java.util.List;
import java.util.Map;

/**
 * created by xinyuan_wang on 2023/2/20
 */
public interface ConsoleLogListener {

    void consoleTimeLogAction(String logType, String planId, String appId, long timeUsed);

    void consoleCountLogAction(String logType, String planId, String appId, long count);

    void consoleSendLogAction(String logType, ReplaySendResult targetSendResult, ReplayActionCaseItem caseItem, long timeUsed);

    void consoleCompareLogAction(String logType, String planId, String appId, String request, long timeUsed);

    boolean isSupported(String logType);

    String generateMessageId(Map<String, String> headers, String url);

    void recordComparison(ReplayActionCaseItem caseItem, List<CategoryComparisonHolder> replayResult);
}
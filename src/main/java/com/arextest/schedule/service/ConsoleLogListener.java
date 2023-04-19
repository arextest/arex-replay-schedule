package com.arextest.schedule.service;


import com.arextest.schedule.comparer.CategoryComparisonHolder;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.sender.ReplaySendResult;

import java.util.List;
import java.util.Map;

/**
 * created by xinyuan_wang on 2023/2/20
 * <p>
 * expose the entrance for use by the successor.
 */
public interface ConsoleLogListener {

    /**
     * record request time by log type, for compare sdk type, q request may be required.
     */
    void consoleTimeLogAction(String logType, String planId, String appId, String request, long timeUsed);

    /**
     * logging count by log type.
     */
    void consoleCountLogAction(String logType, String planId, String appId, long count);

    /**
     * record send log info and invoke time by log type.
     */
    void consoleSendLogAction(String logType, ReplaySendResult targetSendResult, ReplayActionCaseItem caseItem, long timeUsed);

    /**
     * get log message id from url and headers
     */
    String generateMessageId(Map<String, String> headers, String url);

    /**
     * record replay result comparison info
     */
    void recordComparison(ReplayActionCaseItem caseItem, List<CategoryComparisonHolder> replayResult);
}
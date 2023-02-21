package com.arextest.schedule.service;


import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.sender.ReplaySendResult;

import java.util.Map;

/**
 * created by xinyuan_wang on 2023/2/20
 */
public interface ConsoleLogListener {

    void consoleLogAction(long timeUsed, String planId, String planItemId, String type);

    boolean isSupported(String logType);

    void consoleLogAndWriteAction(long timeUsed, String type, ReplaySendResult targetSendResult, ReplayActionCaseItem caseItem);

    String generateMessageId(Map<String, String> headers, String url);

}
package com.arextest.schedule.service;

import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.sender.ReplaySendResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


/**
 * Created by wang_yc on 2021/9/15
 */
@Service
@Slf4j
public class ConsoleLogService {

    private final List<ConsoleLogListener> consoleLogListeners;

    public ConsoleLogService(List<ConsoleLogListener> consoleLogListeners) {
        this.consoleLogListeners = consoleLogListeners;
    }

    public void onConsoleLogEvent(long timeUsed, String logType, String planItemId, ReplayActionItem replayActionItem) {
        ConsoleLogListener listener = find(logType);
        if (listener == null) {
            LOGGER.warn("Could not found consoleLogEvent for {}", logType);
            return;
        }
        listener.consoleLogAction(timeUsed, logType, planItemId, replayActionItem);
    }

    public void consoleLogAndWriteEvent(long timeUsed, String logType, String sendType, ReplaySendResult targetSendResult,
                                        ReplayActionCaseItem caseItem) {
        ConsoleLogListener listener = find(logType);
        if (listener == null) {
            LOGGER.warn("Could not found consoleLogEvent for {}", logType);
            return;
        }
        listener.consoleLogAndWriteAction(timeUsed, sendType, targetSendResult, caseItem);
    }

    public String generateMessageIdEvent(Map<String, String> headers, String url, String type) {
        ConsoleLogListener listener = find(type);
        if (listener == null) {
            LOGGER.warn("Could not found consoleLogEvent for {}", type);
            return null;
        }
        return listener.generateMessageId(headers, url);
    }

    public void staticsFailDetailReasonEvent(ReplayActionCaseItem caseItem, int failType, String logType) {
        ConsoleLogListener listener = find(logType);
        if (listener == null) {
            LOGGER.warn("Could not found consoleLogEvent for {}", logType);
            return;
        }
        listener.staticsFailReason(caseItem, failType);
    }

    public void recordCancelReasonEvent(String planId, String remark, String logType) {
        ConsoleLogListener listener = find(logType);
        if (listener == null) {
            LOGGER.warn("Could not found consoleLogEvent for {}", logType);
            return;
        }
        listener.recordCancelReason(planId, remark);
    }

    public void staticsFailSizeEvent(String planId, long compareFailCount, int failType, String logType) {
        ConsoleLogListener listener = find(logType);
        if (listener == null) {
            LOGGER.warn("Could not found consoleLogEvent for {}", logType);
            return;
        }
        listener.staticsFailSize(planId, compareFailCount, failType);
    }

    private ConsoleLogListener find(String logType) {
        if (CollectionUtils.isNotEmpty(this.consoleLogListeners)) {
            for (ConsoleLogListener listener : this.consoleLogListeners) {
                if (listener.isSupported(logType)) {
                    return listener;
                }
            }
        }
        return null;
    }

}

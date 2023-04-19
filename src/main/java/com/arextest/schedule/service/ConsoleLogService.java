package com.arextest.schedule.service;

import com.arextest.schedule.comparer.CategoryComparisonHolder;
import com.arextest.schedule.model.ReplayActionCaseItem;
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

    /**
     * logging time
     */
    public void onConsoleLogTimeEvent(String logType, String planId, String appId, long timeUsed) {
        ConsoleLogListener listener = find();

        if (listener == null) {
            LOGGER.warn("Could not found consoleLogEvent for {}", logType);
            return;
        }

        listener.consoleTimeLogAction(logType, planId, appId, timeUsed);
    }

    /**
     * logging count
     */
    public void onConsoleLogCountEvent(String logType, String planId, String appId, long count) {
        ConsoleLogListener listener = find();
        if (listener == null) {
            LOGGER.warn("Could not found consoleLogEvent for {}", logType);
            return;
        }

        listener.consoleCountLogAction(logType, planId, appId, count);
    }

    /**
     * record send log and invoke time
     */
    public void onConsoleSendLogEvent(String logType, ReplaySendResult targetSendResult, ReplayActionCaseItem caseItem, long timeUsed) {
        ConsoleLogListener listener = find();
        if (listener == null) {
            LOGGER.warn("Could not found consoleLogEvent for {}", logType);
            return;
        }

        listener.consoleSendLogAction(logType, targetSendResult, caseItem, timeUsed);
    }

    /**
     * the time taken to record the compare sdk
     */
    public void onConsoleCompareLogEvent(String logType, String planId, String appId, String request, long timeUsed) {
        ConsoleLogListener listener = find();
        if (listener == null) {
            LOGGER.warn("Could not found consoleLogEvent for {}", logType);
            return;
        }

        listener.consoleCompareLogAction(logType, planId, appId, request, timeUsed);
    }

    /**
     * get cat log from url and headers
     */
    public String generateMessageIdEvent(Map<String, String> headers, String url, String type) {
        ConsoleLogListener listener = find();
        if (listener == null) {
            LOGGER.warn("Could not found consoleLogEvent for {}", type);
            return null;
        }
        return listener.generateMessageId(headers, url);
    }

    private ConsoleLogListener find() {
        if (CollectionUtils.isNotEmpty(this.consoleLogListeners)) {
            for (ConsoleLogListener listener : this.consoleLogListeners) {
                return listener;
            }
        }
        return null;
    }

    /**
     * todo record the QMessage replay log,  which will be optimized for removal later.
     */
    public void recordComparisonEvent(ReplayActionCaseItem caseItem, List<CategoryComparisonHolder> replayResult, String logType) {
        ConsoleLogListener listener = find();
        if (listener == null) {
            LOGGER.warn("Could not found consoleLogEvent for {}", logType);
            return;
        }
        listener.recordComparison(caseItem, replayResult);
    }
}

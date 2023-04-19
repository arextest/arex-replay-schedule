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
 * Created by xinyuan_wang on 2023/4/19
 */
@Service
@Slf4j
public class ConsoleLogService {

    private final List<ConsoleLogListener> consoleLogListeners;

    public ConsoleLogService(List<ConsoleLogListener> consoleLogListeners) {
        this.consoleLogListeners = consoleLogListeners;
    }

    /**
     * record request time
     */
    public void onConsoleLogTimeEvent(String logType, String planId, String appId, String request, long timeUsed) {
        ConsoleLogListener listener = find();

        if (listener == null) {
            LOGGER.warn("Could not found consoleLogEvent for {}", logType);
            return;
        }

        listener.consoleTimeLogAction(logType, planId, appId, request, timeUsed);
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
     * record send log info and invoke time
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
     * get log message id from url and headers
     */
    public String generateMessageIdEvent(Map<String, String> headers, String url) {
        ConsoleLogListener listener = find();
        if (listener == null) {
            LOGGER.warn("generateMessageId could not found consoleLogEvent");
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
    public void recordComparisonEvent(ReplayActionCaseItem caseItem, List<CategoryComparisonHolder> replayResult) {
        ConsoleLogListener listener = find();
        if (listener == null) {
            LOGGER.warn("recordComparisonEvent could not found consoleLogEvent");
            return;
        }
        listener.recordComparison(caseItem, replayResult);
    }
}

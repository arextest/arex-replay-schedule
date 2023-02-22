package com.arextest.schedule.model;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author jmo
 * @since 2021/10/8
 */
public enum LogType {
    FIND_CASE("findCase", true),
    PREPARE_DEPENDENCY("prepareDependency", true),
    DOSEND("doSend", false),
    COMPARE("compare", true),
    PUSH_COMPARE("pushCompare", true);

    @Getter
    final String value;
    @Getter
    final boolean defaultLog;

    private final static Map<String, LogType> MAP = asMap(LogType::getValue);

    LogType(String value, boolean defaultLog) {
        this.value = value;
        this.defaultLog = defaultLog;
    }

    private static <K> Map<K, LogType> asMap(Function<LogType, K> keySelector) {
        LogType[] values = values();
        Map<K, LogType> mapResult = new HashMap<>(values.length);
        for (int i = 0; i < values.length; i++) {
            LogType type = values[i];
            mapResult.put(keySelector.apply(type), type);
        }
        return mapResult;
    }

    public static LogType of(String value) {
        return MAP.get(value);
    }
}
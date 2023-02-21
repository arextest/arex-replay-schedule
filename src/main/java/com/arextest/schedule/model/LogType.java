package com.arextest.schedule.model;

import lombok.Getter;

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

    LogType(String value, boolean defaultLog) {
        this.value = value;
        this.defaultLog = defaultLog;
    }
}
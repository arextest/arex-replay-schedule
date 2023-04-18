package com.arextest.schedule.model;

import lombok.Getter;

/**
 * created by xinyuan_wang on 2023/4/17
 */
public enum LogType {
    /**
     * task execution delay
     */
    PLAN_EXECUTION_DELAY("planExecutionDelay"),
    /**
     * task execution time
     */
    PLAN_EXECUTION_TIME("planExecutionTime"),
    /**
     * task exception number
     */
    PLAN_EXCEPTION_NUMBER("planExceptionNumber"),
    /**
     * number of execution cases
     */
    CASE_EXECUTION_NUMBER("planExecutionDelay"),
    /**
     * case execution time
     */
    CASE_EXECUTION_TIME("caseExecutionTime"),
    /**
     * case exception number
     */
    CASE_EXCEPTION_NUMBER("caseExceptionNumber"),
    /**
    * paging load case time
     */
    LOAD_CASE_TIME("loadCaseTime"),
    /**
     * switch dependency version time
     */
    SWITCH_DEPENDENCY_VERSION_TIME("switchDependencyVersion"),
    COMPARE("compare"),
    DO_SEND("doSend"),
    COMPARE_SDK("compareSdkTime");

    @Getter
    final String value;

    LogType(String value) {
        this.value = value;
    }
}
package com.arextest.schedule.model;

import lombok.Getter;

/**
 * created by xinyuan_wang on 2023/4/17
 */
public enum LogType {
  /**
   * task execution time
   */
  PLAN_EXECUTION_TIME("planExecutionTime"),
  /**
   * the time from the creation of the task to the actual execution of the task
   */
  PLAN_EXECUTION_DELAY("planExecutionDelay"),
  /**
   * number of abnormal task terminations
   */
  PLAN_EXCEPTION_NUMBER("planExceptionNumber"),
  /**
   * case execution time
   */
  CASE_EXECUTION_TIME("caseExecutionTime"),
  /**
   * number of abnormal case terminations
   */
  CASE_EXCEPTION_NUMBER("caseExceptionNumber"),
  /**
   * time spent pulling cases from the data service.
   */
  LOAD_CASE_TIME("loadCaseTime"),
  /**
   * switch dependency version time
   */
  SWITCH_DEPENDENCY_VERSION_TIME("switchDependencyVersion"),
  /**
   * how long it took to compare the recorded and playback data of the case.
   */
  COMPARE("compare"),
  /**
   * case send request time. eg:
   * com.arextest.schedule.sender.impl.DefaultHttpReplaySender#doInvoke(com.arextest.schedule.sender.SenderParameters)
   */
  DO_SEND("doSend"),
  /**
   * compare sdk request time. eg: COMPARE_INSTANCE.compare(record, result, options)
   */
  COMPARE_SDK("compareSdkTime");

  @Getter
  final String value;

  LogType(String value) {
    this.value = value;
  }
}
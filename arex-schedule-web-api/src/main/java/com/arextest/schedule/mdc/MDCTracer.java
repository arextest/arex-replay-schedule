package com.arextest.schedule.mdc;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

/**
 * @author jmo
 * @since 2021/11/5
 */
public final class MDCTracer {

  private static final String APP_ID = "appId";
  private static final String PLAN_ID = "planId";
  private static final String ACTION_ID = "actionId";
  private static final String DETAIL_ID = "detailId";
  private static final String PLAN_ITEM_ID = "planItemId";
  private static final String EXECUTION_CONTEXT_NAME = "executionContextName";

  private static final String FOR_NOISE_ACTION_ID = "noiseActionId";
  private static final String FOR_NOISE_DETAIL_ID = "noiseDetailId";

  private static final String APP_TYPE = "app-type";
  private static final String AREX_SCHEDULE = "arex-schedule";

  private MDCTracer() {

  }

  private static void add(String name, long value) {
    MDC.put(name, String.valueOf(value));
  }

  public static void addAppType() {
    MDC.put(APP_TYPE, AREX_SCHEDULE);
  }

  public static void addAppId(String appId) {
    addAppType();
    if (StringUtils.isNotEmpty(appId)) {
      MDC.put(APP_ID, appId);
    }
  }

  public static void addPlanId(String planId) {
    addAppType();
    MDC.put(PLAN_ID, planId);
  }

  public static void addExecutionContextNme(String contextName) {
    addAppType();
    MDC.put(EXECUTION_CONTEXT_NAME, contextName);
  }

  public static void addPlanItemId(String planItemId) {
    addAppType();
    MDC.put(PLAN_ITEM_ID, planItemId);
  }

  public static void removePlanItemId() {
    MDC.remove(PLAN_ITEM_ID);
  }

  public static void addPlanId(long planId) {
    addAppType();
    add(PLAN_ID, planId);
  }

  public static void addDetailId(String detailId) {
    addAppType();
    MDC.put(DETAIL_ID, detailId);
  }

  public static void addDetailId(long detailId) {
    addAppType();
    add(DETAIL_ID, detailId);
  }

  public static void removeDetailId() {
    MDC.remove(DETAIL_ID);
  }

  // region the tag for the log of noise identify
  public static void addNoiseActionId(String detailId) {
    addAppType();
    MDC.put(FOR_NOISE_ACTION_ID, detailId);
  }

  public static void removeNoiseActionId() {
    MDC.remove(FOR_NOISE_ACTION_ID);
  }

  public static void addNoiseDetailId(String detailId) {
    addAppType();
    MDC.put(FOR_NOISE_DETAIL_ID, detailId);
  }

  public static void removeNoiseDetailId() {
    MDC.remove(FOR_NOISE_DETAIL_ID);
  }
  // endregion

  public static void clear() {
    MDC.clear();
  }
}
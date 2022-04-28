package com.arextest.replay.schedule.mdc;

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

    private MDCTracer() {

    }

    private static void add(String name, long value) {
        MDC.put(name, String.valueOf(value));
    }

    public static void addAppId(String appId) {
        if (StringUtils.isNotEmpty(appId)) {
            MDC.put(APP_ID, appId);
        }
    }

    public static void addPlanId(String planId) {
        MDC.put(PLAN_ID, planId);
    }

    public static void addPlanId(long planId) {
        add(PLAN_ID, planId);
    }

    public static void addActionId(long actionId) {
        add(ACTION_ID, actionId);
    }

    public static void addDetailId(String detailId) {
        MDC.put(DETAIL_ID, detailId);
    }

    public static void addDetailId(long detailId) {
        add(DETAIL_ID, detailId);
    }

    public static void removeDetailId() {
        MDC.remove(DETAIL_ID);
    }

    public static void clear() {
        MDC.clear();
    }
}

package com.arextest.schedule.model.bizlog;

import lombok.Getter;

import java.text.MessageFormat;

/**
 * Created by qzmo on 2023/5/31.
 */
public enum BizLogContent {
    PLAN_START(0, "Plan passes validation, starts execution."),
    PLAN_CASE_SAVED(1, "Plan saved {0} cases to send."),
    PLAN_CONTEXT_BUILT(2, "{0} execution context built."),
    PLAN_REPORT_INIT(3, "Plan report init."),
    PLAN_ASYNC_RUN_START(4, "Plan async task init."),
    PLAN_STATUS_CHANGE(5, "Plan status changed to {0}, because of [{1}]."),


    QPS_LIMITER_INIT(100, "Qps limiter init with initial total rate of {0} for {1} instances."),
    QPS_LIMITER_CHANGE(101, ""),
    QPS_LIMITER_STATUS(102, ""),

    CONTEXT_START(200, "Context: {0} init with action: {1}, before hook took {2} ms."),
    CONTEXT_AFTER_RUN(202, ""),
    CONTEXT_AFTER_HOOK(203, ""),

    ACTION_ITEM_EXECUTE_CONTEXT(300, "Action {0} under context: {1} starts executing."),
    ACTION_ITEM_SKIP_CONTEXT(301, ""),
    ACTION_ITEM_INIT_TOTAL_COUNT(302, "Action item id: {0} init total case count: {1}."),
    ACTION_ITEM_STATUS_CHANGED(303, "Action item status changed to {0}, because of [{1}]."),
    ACTION_ITEM_SENT(304, "All cases of action item sent, total size: {0}"),
    ACTION_ITEM_INTERRUPTED(305, "Action item status interrupted, because Qps limiter with total error count of: {0} and continuous error of: {1}."),


    ;
    BizLogContent(int type, String template) {
        this.type = type;
        this.template = template;
    }

    @Getter
    private String template;

    @Getter
    private int type;

    public String format(Object... args) {
        try {
            return MessageFormat.format(this.getTemplate(), args);
        } catch (Exception e) {
            return this.getTemplate();
        }
    }
}

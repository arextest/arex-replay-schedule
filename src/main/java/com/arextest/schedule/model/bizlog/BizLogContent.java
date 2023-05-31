package com.arextest.schedule.model.bizlog;

import lombok.Getter;

import java.text.MessageFormat;

/**
 * Created by qzmo on 2023/5/31.
 */
public enum BizLogContent {
    PLAN_START(0, BizLogLevel.INFO, "Plan passes validation, starts execution."),
    PLAN_CASE_SAVED(1, BizLogLevel.INFO, ""),
    PLAN_CONTEXT_BUILT(2, BizLogLevel.INFO, ""),

    QPS_LIMITER_INIT(100, BizLogLevel.INFO, ""),
    QPS_LIMITER_CHANGE(101, BizLogLevel.INFO, ""),
    QPS_LIMITER_STATUS(102, BizLogLevel.INFO, ""),

    CONTEXT_START(200, BizLogLevel.INFO, ""),
    CONTEXT_INIT_HOOK(201, BizLogLevel.INFO, ""),
    CONTEXT_AFTER_RUN(202, BizLogLevel.INFO, ""),
    CONTEXT_AFTER_HOOK(203, BizLogLevel.INFO, ""),

    ACTION_ITEM_EXECUTE_CONTEXT(300, BizLogLevel.INFO, ""),
    ACTION_ITEM_SKIP_CONTEXT(301, BizLogLevel.INFO, ""),
    ;
    BizLogContent(int type, BizLogLevel level, String template) {
        this.type = type;
        this.level = level;
        this.template = template;
    }

    @Getter
    private String template;

    @Getter
    private int type;

    private BizLogLevel level;

    public String format(Object... args) {
        return MessageFormat.format(this.getTemplate(), args);
    }
}

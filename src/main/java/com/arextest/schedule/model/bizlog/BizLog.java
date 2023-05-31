package com.arextest.schedule.model.bizlog;

import com.arextest.schedule.model.ReplayPlan;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Created by qzmo on 2023/5/31.
 */
@Data
@Builder
public class BizLog {
    private Date date;
    private int level;
    private String message;
    private int logType;

    private String planId;
    private String contextName;
    private String contextIdentifier;
    private String caseItemId;

    // private Throwable exception;

    public static BizLogBuilder constructBase() {
        return BizLog.builder().date(new Date());
    }

    public static BizLogBuilder info() {
        return BizLog.constructBase().level(BizLogLevel.INFO.getVal());
    }

    public static BizLogBuilder warn() {
        return BizLog.constructBase().level(BizLogLevel.WARN.getVal());
    }

    public static BizLogBuilder error() {
        return BizLog.constructBase().level(BizLogLevel.ERROR.getVal());
    }

    public static void recordPlanStart(ReplayPlan plan) {
        plan.enqueueBizLog(BizLog.info()
                .planId(plan.getId())
                .logType(BizLogContent.PLAN_START.getType())
                .message(BizLogContent.PLAN_START.format())
                .build());
    }
}

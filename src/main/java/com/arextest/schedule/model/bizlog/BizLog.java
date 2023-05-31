package com.arextest.schedule.model.bizlog;

import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class BizLog {
    private Date date;
    private BizLogLevel level;
    private String message;
    private int logType;

    private ReplayPlan plan;
    private PlanExecutionContext context;
    private ReplayActionItem actionItem;

    private Throwable exception;

    public static BizLogBuilder constructBase() {
        return BizLog.builder().date(new Date());
    }

    public static BizLogBuilder info() {
        return BizLog.constructBase().level(BizLogLevel.INFO);
    }

    public static BizLogBuilder warn() {
        return BizLog.constructBase().level(BizLogLevel.WARN);
    }

    public static BizLogBuilder error() {
        return BizLog.constructBase().level(BizLogLevel.ERROR);
    }

}

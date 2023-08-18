package com.arextest.schedule.model.bizlog;

import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;

/**
 * Created by qzmo on 2023/5/31.
 */
@Data
@Builder
@Slf4j
@FieldNameConstants
public class BizLog {
    private Date date;
    private int level;
    private String message;
    private int logType;

    private String planId;
    private boolean resumedExecution;
    private String contextName;

    private String contextIdentifier;
    private String caseItemId;
    private String actionItemId;
    private String operationName;

    private String exception;
    private String request;
    private String response;
    private String traceId;
    private String extra;
    private Boolean reRun;

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
    public static BizLogBuilder debug() {
        return BizLog.constructBase().level(BizLogLevel.DEBUG.getVal());
    }

    public void postProcessAndEnqueue(ReplayPlan plan) {
        if (plan == null) {
            LOGGER.warn("Biz logger got null plan");
            return;
        }
        this.setPlanId(plan.getId());
        this.setResumedExecution(plan.isResumed());
        this.setReRun(plan.isReRun());
        plan.enqueueBizLog(this);
    }

    @SuppressWarnings("rawtypes")
    public void postProcessAndEnqueue(PlanExecutionContext context) {
        if (context == null) {
            LOGGER.warn("Biz logger got null context");
            return;
        }
        this.setContextName(context.getContextName());
        this.postProcessAndEnqueue(context.getPlan());
    }

    public void postProcessAndEnqueue(ReplayActionItem action) {
        if (action == null) {
            LOGGER.warn("Biz logger got null action");
            return;
        }
        this.setActionItemId(action.getId());
        this.setOperationName(action.getOperationName());
        Optional.ofNullable(action.getParent()).ifPresent(this::postProcessAndEnqueue);
    }

    public void postProcessAndEnqueue(ReplayActionCaseItem caseItem) {
        if (caseItem == null) {
            LOGGER.warn("Biz logger got null caseItem");
            return;
        }
        this.setContextIdentifier(caseItem.getContextIdentifier());
        this.setCaseItemId(caseItem.getId());
        Optional.ofNullable(caseItem.getParent()).ifPresent(this::postProcessAndEnqueue);
    }
}

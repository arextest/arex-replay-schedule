package com.arextest.schedule.model.bizlog;

import com.arextest.schedule.common.SendSemaphoreLimiter;
import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
    private String actionItemId;

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

    private void postProcessAndEnqueue(ReplayPlan plan) {
        this.setPlanId(plan.getId());
        plan.enqueueBizLog(this);
    }

    private void postProcessAndEnqueue(PlanExecutionContext context) {
        this.postProcessAndEnqueue(context.getPlan());
        this.setContextName(context.getContextName());
    }

    private void postProcessAndEnqueue(ReplayActionItem action) {
        Optional.ofNullable(action.getParent()).ifPresent(this::postProcessAndEnqueue);
        this.setActionItemId(action.getId());
    }

    private void postProcessAndEnqueue(ReplayActionCaseItem caseItem) {
        Optional.ofNullable(caseItem.getParent()).ifPresent(this::postProcessAndEnqueue);
        this.setContextIdentifier(caseItem.getContextIdentifier());
        this.setCaseItemId(caseItem.getId());
    }

    // region <Plan Level Log>
    public static void recordPlanStart(ReplayPlan plan) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.PLAN_START.getType())
                .message(BizLogContent.PLAN_START.format())
                .build();

        log.postProcessAndEnqueue(plan);
    }

    public static void recordReportInit(ReplayPlan plan) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.PLAN_REPORT_INIT.getType())
                .message(BizLogContent.PLAN_REPORT_INIT.format())
                .build();

        log.postProcessAndEnqueue(plan);
    }

    public static void recordPlanAsyncStart(ReplayPlan plan) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.PLAN_ASYNC_RUN_START.getType())
                .message(BizLogContent.PLAN_ASYNC_RUN_START.format())
                .build();

        log.postProcessAndEnqueue(plan);
    }

    public static void recordPlanCaseSaved(ReplayPlan plan, int size) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.PLAN_CASE_SAVED.getType())
                .message(BizLogContent.PLAN_CASE_SAVED.format(size))
                .build();

        log.postProcessAndEnqueue(plan);
    }

    public static void recordPlanStatusChange(ReplayPlan plan, String targetStatus, String message) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.PLAN_STATUS_CHANGE.getType())
                .message(BizLogContent.PLAN_STATUS_CHANGE.format(targetStatus, message))
                .build();

        log.postProcessAndEnqueue(plan);
    }

    // endregion

    // region <Action Level Log>
    public static void recordActionUnderContext(ReplayActionItem action, PlanExecutionContext context) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.ACTION_ITEM_EXECUTE_CONTEXT.getType())
                .message(BizLogContent.ACTION_ITEM_EXECUTE_CONTEXT.format(action.getId(), context.getContextName()))
                .build();

        log.postProcessAndEnqueue(action);
    }

    public static void recordActionItemCaseCount(ReplayActionItem action) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.ACTION_ITEM_INIT_TOTAL_COUNT.getType())
                .message(BizLogContent.ACTION_ITEM_INIT_TOTAL_COUNT.format(action.getId(), action.getReplayCaseCount()))
                .build();

        log.postProcessAndEnqueue(action);
    }

    public static void recordActionStatusChange(ReplayActionItem action, String targetStatus, String reason) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.ACTION_ITEM_STATUS_CHANGED.getType())
                .message(BizLogContent.ACTION_ITEM_STATUS_CHANGED.format(targetStatus, reason))
                .build();

        log.postProcessAndEnqueue(action);
    }

    public static void recordActionInterrupted(ReplayActionItem action) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.ACTION_ITEM_INTERRUPTED.getType())
                .message(BizLogContent.ACTION_ITEM_INTERRUPTED.format(action.getSendRateLimiter().totalError(),
                        action.getSendRateLimiter().continuousError()))
                .build();

        log.postProcessAndEnqueue(action);
    }

    public static void recordActionItemSent(ReplayActionItem action) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.ACTION_ITEM_SENT.getType())
                .message(BizLogContent.ACTION_ITEM_SENT.format(action.getCaseProcessCount()))
                .build();

        log.postProcessAndEnqueue(action);
    }
    // endregion

    // region <QPS>
    public static void recordQpsInit(ReplayPlan plan, int initQps, int instanceCount) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.QPS_LIMITER_INIT.getType())
                .message(BizLogContent.QPS_LIMITER_INIT.format(initQps, instanceCount))
                .build();

        log.postProcessAndEnqueue(plan);
    }
    // endregion

    // region <Context Level Log>
    public static void recordContextBuilt(ReplayPlan plan) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.PLAN_CONTEXT_BUILT.getType())
                .message(BizLogContent.PLAN_CONTEXT_BUILT.format(Optional.ofNullable(plan.getExecutionContexts())
                        .map(Collection::size).orElse(0)))
                .build();

        log.postProcessAndEnqueue(plan);
    }

    public static void recordContextBeforeRun(PlanExecutionContext context, long elapsed) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.CONTEXT_START.getType())
                .message(BizLogContent.CONTEXT_START.format(context.getContextName(),
                        context.getActionType().name(), elapsed))
                .build();

        log.postProcessAndEnqueue(context);
    }
    // endregion
}

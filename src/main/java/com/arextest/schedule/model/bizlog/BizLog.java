package com.arextest.schedule.model.bizlog;

import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

/**
 * Created by qzmo on 2023/5/31.
 */
@Data
@Builder
@SuppressWarnings("rawtypes")
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
    private String extra;

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
        this.setPlanId(plan.getId());
        this.setResumedExecution(plan.isResumed());
        plan.enqueueBizLog(this);
    }

    public void postProcessAndEnqueue(PlanExecutionContext context) {
        this.setContextName(context.getContextName());
        this.postProcessAndEnqueue(context.getPlan());
    }

    public void postProcessAndEnqueue(ReplayActionItem action) {
        this.setActionItemId(action.getId());
        this.setOperationName(action.getOperationName());
        Optional.ofNullable(action.getParent()).ifPresent(this::postProcessAndEnqueue);
    }

    public void postProcessAndEnqueue(ReplayActionCaseItem caseItem) {
        this.setContextIdentifier(caseItem.getContextIdentifier());
        this.setCaseItemId(caseItem.getId());
        Optional.ofNullable(caseItem.getParent()).ifPresent(this::postProcessAndEnqueue);
    }

    // region <Plan Level Log>
    public static void recordPlanStart(ReplayPlan plan) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.PLAN_START.getType())
                .message(BizLogContent.PLAN_START.format())
                .build();

        log.postProcessAndEnqueue(plan);
    }

    public static void recordPlanDone(ReplayPlan plan) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.PLAN_DONE.getType())
                .message(BizLogContent.PLAN_DONE.format())
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

    public static void recordPlanException(ReplayPlan plan, Throwable t) {
        BizLog log = BizLog.error()
                .logType(BizLogContent.PLAN_FATAL_ERROR.getType())
                .message(BizLogContent.PLAN_FATAL_ERROR.format())
                .exception(BizLogContent.throwableToString(t))
                .build();

        log.postProcessAndEnqueue(plan);
    }
    // endregion

    // region <Action Level Log>
    public static void recordActionUnderContext(ReplayActionItem action, PlanExecutionContext context) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.ACTION_ITEM_EXECUTE_CONTEXT.getType())
                .message(BizLogContent.ACTION_ITEM_EXECUTE_CONTEXT.format(
                        action.getOperationName(),
                        action.getId(),
                        context.getContextName(),
                        context.getActionType().name()))
                .build();

        log.postProcessAndEnqueue(action);
    }

    public static void recordActionItemCaseCount(ReplayActionItem action) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.ACTION_ITEM_INIT_TOTAL_COUNT.getType())
                .message(BizLogContent.ACTION_ITEM_INIT_TOTAL_COUNT.format(
                        action.getOperationName(),
                        action.getId(),
                        action.getReplayCaseCount()))
                .build();

        log.postProcessAndEnqueue(action);
    }

    public static void recordActionStatusChange(ReplayActionItem action, String targetStatus, String reason) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.ACTION_ITEM_STATUS_CHANGED.getType())
                .message(BizLogContent.ACTION_ITEM_STATUS_CHANGED.format(
                        action.getOperationName(),
                        action.getId(),
                        targetStatus,
                        reason))
                .build();

        log.postProcessAndEnqueue(action);
    }

    public static void recordActionItemSent(ReplayActionItem action) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.ACTION_ITEM_SENT.getType())
                .message(BizLogContent.ACTION_ITEM_SENT.format(
                        action.getOperationName(),
                        action.getId(),
                        action.getCaseProcessCount()))
                .build();

        log.postProcessAndEnqueue(action);
    }

    public static void recordActionItemBatchSent(ReplayActionItem action, int batchSize) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.ACTION_ITEM_BATCH_SENT.getType())
                .message(BizLogContent.ACTION_ITEM_BATCH_SENT.format(
                        action.getOperationName(),
                        action.getId(),
                        batchSize))
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

    public static void recordQPSChange(ReplayPlan plan, int source, int target) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.QPS_LIMITER_CHANGE.getType())
                .message(BizLogContent.QPS_LIMITER_CHANGE.format(source, target))
                .build();
        if (plan != null) {
            log.postProcessAndEnqueue(plan);
        }
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

    public static void recordContextAfterRun(PlanExecutionContext context, long elapsed) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.CONTEXT_AFTER_RUN.getType())
                .message(BizLogContent.CONTEXT_AFTER_RUN.format(context.getContextName(), elapsed))
                .build();

        log.postProcessAndEnqueue(context);
    }
    // endregion

    // region <Resume run Log>
    public static void recordResumeRun(ReplayPlan plan) {
        BizLog log = BizLog.info()
                .logType(BizLogContent.RESUME_START.getType())
                .message(BizLogContent.RESUME_START.format(plan.getReplayActionItemList().size()))
                .build();

        log.postProcessAndEnqueue(plan);
    }
    // endregion

    public enum BizLogContent {
        PLAN_START(0, "Plan passes validation, starts execution."),
        PLAN_CASE_SAVED(1, "Plan saved {0} cases to send."),
        PLAN_CONTEXT_BUILT(2, "{0} execution context built."),
        PLAN_DONE(3, "Plan send job done normally."),
        PLAN_ASYNC_RUN_START(4, "Plan async task init."),
        PLAN_STATUS_CHANGE(5, "Plan status changed to {0}, because of [{1}]."),
        PLAN_FATAL_ERROR(6, "Plan execution encountered unchecked exception or error."),


        QPS_LIMITER_INIT(100, "Qps limiter init with initial total rate of {0} for {1} instances."),
        QPS_LIMITER_CHANGE(101, "Qps limit changed from {0} to {1}."),

        CONTEXT_START(200, "Context: {0} init with action: {1}, before hook took {2} ms."),
        CONTEXT_AFTER_RUN(202, "Context: {0} done, after hook took {1} ms."),
        CONTEXT_SKIP(203, "Context: {0} done, after hook took {1} ms."),
        CONTEXT_NORMAL(204, "Context: {0} done, after hook took {1} ms."),

        ACTION_ITEM_EXECUTE_CONTEXT(300, "Operation: {0} id: {1} under context: {2} starts executing action type: {3}."),
        ACTION_ITEM_INIT_TOTAL_COUNT(302, "Operation: {0} id: {1} init total case count: {2}."),
        ACTION_ITEM_STATUS_CHANGED(303, "Operation: {0} id: {1} status changed to {2}, because of [{3}]."),
        ACTION_ITEM_SENT(304, "All cases of Operation: {0} id: {1} sent, total size: {2}"),
        ACTION_ITEM_BATCH_SENT(305, "Batch cases of Operation: {0} id: {1} sent, size: {2}"),

        RESUME_START(400, "Plan resumed with action size of {0}"),

        ;
        BizLogContent(int type, String template) {
            this.type = type;
            this.template = template;
        }

        @Getter
        private final String template;

        @Getter
        private final int type;

        public String format(Object... args) {
            try {
                return MessageFormat.format(this.getTemplate(), args);
            } catch (Exception e) {
                return this.getTemplate();
            }
        }

        public static String throwableToString(Throwable throwable) {
            return ExceptionUtils.getStackTrace(throwable);
        }
    }
}

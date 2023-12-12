package com.arextest.schedule.bizlog;

import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.bizlog.BizLog;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Created by Qzmo on 2023/6/8
 */
public class BizLogger {

  // region <Plan Level Log>
  public static void recordPlanStart(ReplayPlan plan) {
    BizLog log = BizLog.info().logType(BizLogContent.PLAN_START.getType())
        .message(BizLogContent.PLAN_START.format()).build();

    log.postProcessAndEnqueue(plan);
  }

  public static void recordPlanDone(ReplayPlan plan) {
    BizLog log =
        BizLog.info().logType(BizLogContent.PLAN_DONE.getType())
            .message(BizLogContent.PLAN_DONE.format()).build();

    log.postProcessAndEnqueue(plan);
  }

  public static void recordPlanAsyncStart(ReplayPlan plan) {
    BizLog log = BizLog.info().logType(BizLogContent.PLAN_ASYNC_RUN_START.getType())
        .message(BizLogContent.PLAN_ASYNC_RUN_START.format()).build();

    log.postProcessAndEnqueue(plan);
  }

  public static void recordPlanCaseSaved(ReplayPlan plan, int size, long elapsed) {
    BizLog log = BizLog.debug().logType(BizLogContent.PLAN_CASE_SAVED.getType())
        .message(BizLogContent.PLAN_CASE_SAVED.format(size, elapsed)).build();

    log.postProcessAndEnqueue(plan);
  }

  public static void recordPlanStatusChange(ReplayPlan plan, String targetStatus, String message) {
    BizLog log = BizLog.info().logType(BizLogContent.PLAN_STATUS_CHANGE.getType())
        .message(BizLogContent.PLAN_STATUS_CHANGE.format(targetStatus, message)).build();

    log.postProcessAndEnqueue(plan);
  }

  public static void recordPlanException(ReplayPlan plan, Throwable t) {
    BizLog log = BizLog.error().logType(BizLogContent.PLAN_FATAL_ERROR.getType())
        .message(BizLogContent.PLAN_FATAL_ERROR.format())
        .exception(BizLogContent.throwableToString(t)).build();

    log.postProcessAndEnqueue(plan);
  }
  // endregion


  public static void recordActionItemCaseCount(ReplayActionItem action) {
    BizLog log = BizLog.debug().logType(BizLogContent.ACTION_ITEM_INIT_TOTAL_COUNT.getType())
        .message(BizLogContent.ACTION_ITEM_INIT_TOTAL_COUNT.format(action.getOperationName(),
            action.getId(),
            action.getReplayCaseCount()))
        .build();

    log.postProcessAndEnqueue(action);
  }

  public static void recordActionItemCaseReRunCount(ReplayActionItem action) {
    BizLog log = BizLog.debug().logType(BizLogContent.ACTION_ITEM_INIT_TOTAL_RERUN_COUNT.getType())
        .message(BizLogContent.ACTION_ITEM_INIT_TOTAL_RERUN_COUNT.format(action.getOperationName(),
            action.getId(),
            action.getRerunCaseCount()))
        .build();

    log.postProcessAndEnqueue(action);
  }

  // region <QPS>
  public static void recordQpsInit(ReplayPlan plan, int initQps, int instanceCount) {
    BizLog log = BizLog.debug().logType(BizLogContent.QPS_LIMITER_INIT.getType())
        .message(BizLogContent.QPS_LIMITER_INIT.format(initQps, instanceCount)).build();

    log.postProcessAndEnqueue(plan);
  }

  public static void recordQPSChange(ReplayPlan plan, int source, int target) {
    BizLog log = BizLog.debug().logType(BizLogContent.QPS_LIMITER_CHANGE.getType())
        .message(BizLogContent.QPS_LIMITER_CHANGE.format(source, target)).build();
    if (plan != null) {
      log.postProcessAndEnqueue(plan);
    }
  }

  public static void recordQPSReset(@NotNull ReplayPlan plan, int target) {
    BizLog log = BizLog.debug().logType(BizLogContent.QPS_LIMITER_RESET.getType())
        .message(BizLogContent.QPS_LIMITER_RESET.format(target)).build();
    log.postProcessAndEnqueue(plan);
  }
  // endregion

  // region <Context Level Log>
  public static void recordContextBuilt(ReplayPlan plan, long elapsed) {
    BizLog log = BizLog.debug().logType(BizLogContent.PLAN_CONTEXT_BUILT.getType())
        .message(BizLogContent.PLAN_CONTEXT_BUILT
            .format(
                Optional.ofNullable(plan.getExecutionContexts()).map(Collection::size).orElse(0),
                elapsed))
        .build();

    log.postProcessAndEnqueue(plan);
  }

  public static void recordContextBeforeRun(PlanExecutionContext<?> context, long elapsed) {
    BizLog log = BizLog.debug().logType(BizLogContent.CONTEXT_START.getType())
        .message(
            BizLogContent.CONTEXT_START.format(context.getContextName(),
                context.getActionType().name(), elapsed))
        .build();

    log.postProcessAndEnqueue(context);
  }

  public static void recordContextAfterRun(PlanExecutionContext<?> context, long elapsed) {
    BizLog log = BizLog.debug().logType(BizLogContent.CONTEXT_AFTER_RUN.getType())
        .message(BizLogContent.CONTEXT_AFTER_RUN.format(context.getContextName(), elapsed)).build();

    log.postProcessAndEnqueue(context);
  }

  public static void recordContextSkipped(PlanExecutionContext<?> context,
      ReplayActionItem actionItem,
      long skipCount) {
    BizLog log = BizLog.info().logType(BizLogContent.CONTEXT_SKIP.getType())
        .message(BizLogContent.CONTEXT_SKIP.format(context.getContextName(), actionItem.getId(),
            skipCount))
        .build();

    log.postProcessAndEnqueue(context);
  }

  public static void recordContextProcessedNormal(PlanExecutionContext<?> context, long sentCount) {
    BizLog log = BizLog.debug().logType(BizLogContent.CONTEXT_NORMAL.getType())
        .message(BizLogContent.CONTEXT_NORMAL.format(context.getContextName(), sentCount)).build();

    log.postProcessAndEnqueue(context);
  }

  public static void recordContextPrepareFailure(PlanExecutionContext<?> context, Throwable t) {
    BizLog log = BizLog.error().logType(BizLogContent.CONTEXT_PREPARE_ERR.getType())
        .message(BizLogContent.CONTEXT_PREPARE_ERR.format(context.getContextName(), t.getMessage()))
        .exception(BizLogContent.throwableToString(t)).build();

    log.postProcessAndEnqueue(context);
  }
  // endregion

  // region <Resume run Log>
  public static void recordResumeRun(ReplayPlan plan) {
    BizLog log = BizLog.info().logType(BizLogContent.RESUME_START.getType())
        .message(BizLogContent.RESUME_START.format(plan.getReplayActionItemList().size())).build();

    log.postProcessAndEnqueue(plan);
  }
  // endregion

  // region <noise identify Log>
  public static void recordCaseForNoiseSendStart(@NotNull PlanExecutionContext<?> context,
      int sentCount) {
    BizLog log = BizLog.debug().logType(BizLogContent.NOISE_IDENTIFY_CASE_SEND_START.getType())
        .message(BizLogContent.NOISE_IDENTIFY_CASE_SEND_START.format(context.getContextName(),
            sentCount)).build();
    log.postProcessAndEnqueue(context);
  }

  public static void recordCaseForNoiseSendFinish(@NotNull PlanExecutionContext<?> context,
      int sentCount, long elapsedMills) {
    BizLog log = BizLog.debug().logType(BizLogContent.NOISE_IDENTIFY_CASE_SEND_FINISH.getType())
        .message(BizLogContent.NOISE_IDENTIFY_CASE_SEND_FINISH.format(context.getContextName(),
            sentCount, elapsedMills)).build();
    log.postProcessAndEnqueue(context);
  }
  // endregion

  @Getter
  public enum BizLogContent {
    PLAN_START(0, "Plan passes validation, starts building replay report."),
    PLAN_CASE_SAVED(1, "Plan saved total {0} cases to send, took {1} ms."),
    PLAN_CONTEXT_BUILT(2, "{0} execution context built, took {1} ms."),
    PLAN_DONE(3, "Plan send job done normally."),
    PLAN_ASYNC_RUN_START(4, "Plan async task init, starts processing cases."),
    PLAN_STATUS_CHANGE(5, "Plan status changed to {0}, because of [{1}]."),
    PLAN_FATAL_ERROR(6, "Plan execution encountered unchecked exception or error, "
        + "please contact Arex admins"),

    QPS_LIMITER_INIT(100,
        "Qps limiter init with initial total rate of {0} for {1} instances."),
    QPS_LIMITER_CHANGE(101, "Qps limit changed from {0} to {1}."),
    QPS_LIMITER_RESET(102, "Qps limit will reset to initial rate {0}."),

    CONTEXT_START(200, "Context: {0} init with action: {1}, before hook took {2} ms."),
    CONTEXT_AFTER_RUN(202, "Context: {0} done, after hook took {1} ms."),
    CONTEXT_SKIP(203, "Context: {0}, Action: {1}, skipped {2} cases "),
    CONTEXT_NORMAL(204, "Context: {0}, execute normal, {1} cases sent."),
    CONTEXT_PREPARE_ERR(205, "Context: {0}, prepare remote dependency failed due to {1}."),

    @Deprecated
    ACTION_ITEM_CASE_SAVED(306, "Operation {0} saved total {1} cases to send, took {2} ms."),

    @Deprecated
    ACTION_ITEM_EXECUTE_CONTEXT(300,
        "Operation: {0} id: {1} under context: {2} starts executing action type: {3}."),
    ACTION_ITEM_INIT_TOTAL_RERUN_COUNT(301, "Operation: {0} id: {1} rerun total case count: {2}."),
    ACTION_ITEM_INIT_TOTAL_COUNT(302, "Operation: {0} id: {1} init total case count: {2}."),
    @Deprecated
    ACTION_ITEM_STATUS_CHANGED(303,
        "Operation: {0} id: {1} status changed to {2}, because of [{3}]."),
    @Deprecated
    ACTION_ITEM_SENT(304, "All cases of Operation: {0} id: {1} sent, total size: {2}"),
    @Deprecated
    ACTION_ITEM_BATCH_SENT(305, "Batch cases of Operation: {0} id: {1} sent, size: {2}"),

    RESUME_START(400, "Plan resumed with operation size of {0}"),

    NOISE_IDENTIFY_CASE_SEND_START(500, "Context: {0}, {1} case start sending to identify noise."),
    NOISE_IDENTIFY_CASE_SEND_FINISH(501,
        "Context: {0}, {1} case finish sending to identify noise, took {2} ms."),

    ;

    private final String template;
    private final int type;

    BizLogContent(int type, String template) {
      this.type = type;
      this.template = template;
    }

    public static String throwableToString(Throwable throwable) {
      return ExceptionUtils.getStackTrace(throwable);
    }

    public String format(Object... args) {
      try {
        return MessageFormat.format(this.getTemplate(), args);
      } catch (Exception e) {
        return this.getTemplate();
      }
    }
  }
}

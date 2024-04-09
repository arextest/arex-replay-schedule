package com.arextest.schedule.bizlog;

import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.model.bizlog.BizLog;
import java.text.MessageFormat;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
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

  public static void recordPlanStatusChange(ReplayPlan plan, ReplayStatusType status) {
    recordPlanStatusChange(plan, status, null);
  }

  public static void recordPlanStatusChange(ReplayPlan plan, ReplayStatusType status, String message) {
    if (StringUtils.isEmpty(message)) {
      switch (status) {
        case FAIL_INTERRUPTED:
          message =
              "Plan Interrupted because there are 40+ continuous failure or more than 10% of cases failed. "
                  + "Please check the detail of invalid cases in the report.";
          break;
        case CANCELLED:
          message = "Plan Cancelled by user.";
          break;
        default:
          break;
      }
    }

    BizLog log = BizLog.info().logType(BizLogContent.PLAN_STATUS_CHANGE.getType())
        .message(BizLogContent.PLAN_STATUS_CHANGE.format(status.name(), message)).build();

    log.postProcessAndEnqueue(plan);
  }

  public static void recordPlanException(ReplayPlan plan, Throwable t) {
    BizLog log = BizLog.error().logType(BizLogContent.PLAN_FATAL_ERROR.getType())
        .message(BizLogContent.PLAN_FATAL_ERROR.format())
        .exception(BizLogContent.throwableToString(t)).build();

    log.postProcessAndEnqueue(plan);
  }
  // endregion


  // region <Context Level Log>

  public static void recordContextSkipped(PlanExecutionContext<?> context,
      ReplayActionItem actionItem,
      long skipCount) {
    BizLog log = BizLog.warn().logType(BizLogContent.CONTEXT_SKIP.getType())
        .message(BizLogContent.CONTEXT_SKIP.format(context.getContextName(), actionItem.getId(),
            skipCount))
        .build();

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


  @Getter
  public enum BizLogContent {
    PLAN_START(0, "Plan passes validation, starts building replay report."),
    @Deprecated
    PLAN_CASE_SAVED(1, "Plan saved total {0} cases to send, took {1} ms."),
    @Deprecated
    PLAN_CONTEXT_BUILT(2, "{0} execution context built, took {1} ms."),
    PLAN_DONE(3, "Plan send job done normally."),
    PLAN_ASYNC_RUN_START(4, "Plan async task init, starts processing cases."),
    PLAN_STATUS_CHANGE(5, "Plan status changed to {0}, because of [{1}]."),
    PLAN_FATAL_ERROR(6, "Plan execution encountered unchecked exception or error, "
        + "please contact Arex admins"),

    @Deprecated
    QPS_LIMITER_INIT(100,
        "Qps limiter init with initial total rate of {0} for {1} instances."),
    @Deprecated
    QPS_LIMITER_CHANGE(101, "Qps limit changed from {0} to {1}."),
    @Deprecated
    QPS_LIMITER_RESET(102, "Qps limit will reset to initial rate {0}."),
    @Deprecated
    CONTEXT_START(200, "Config Context: {0} init with action: {1}, before hook took {2} ms."),
    @Deprecated
    CONTEXT_AFTER_RUN(202, "Config Context: {0} done, after hook took {1} ms."),
    CONTEXT_SKIP(203, "Config Context: {0}, Action: {1}, skipped {2} cases "),
    @Deprecated
    CONTEXT_NORMAL(204, "Config Context: {0}, execute normal, {1} cases sent."),
    CONTEXT_PREPARE_ERR(205,
        "Config Context: {0}, prepare remote dependency failed, skip cases with this config version,"
            + " please check if the target service is healthy and Arex service can access it. Original error message: {1}."),

    @Deprecated
    ACTION_ITEM_CASE_SAVED(306, "Operation {0} saved total {1} cases to send, took {2} ms."),

    @Deprecated
    ACTION_ITEM_EXECUTE_CONTEXT(300,
        "Operation: {0} id: {1} under context: {2} starts executing action type: {3}."),
    @Deprecated
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

    @Deprecated
    NOISE_IDENTIFY_CASE_SEND_START(500, "Context: {0}, {1} case start sending to identify noise."),
    @Deprecated
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

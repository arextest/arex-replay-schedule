package com.arextest.schedule.service;

import static com.arextest.schedule.common.CommonConstant.DEFAULT_COUNT;
import com.arextest.schedule.common.SendSemaphoreLimiter;
import com.arextest.schedule.mdc.AbstractTracedRunnable;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.CaseSendStatusType;
import com.arextest.schedule.model.ExecutionStatus;
import com.arextest.schedule.model.LogType;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.sender.ReplaySender;
import java.util.concurrent.CountDownLatch;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jmo
 * @since 2022/2/20
 */
@Slf4j
@Setter
final class AsyncSendCaseTaskRunnable extends AbstractTracedRunnable {

  private transient final ReplayCaseTransmitService transmitService;
  private transient ReplaySender replaySender;
  private transient ReplayActionCaseItem caseItem;
  private transient CountDownLatch groupSentLatch;
  private transient SendSemaphoreLimiter limiter;
  private transient ExecutionStatus executionStatus;
  private transient MetricService metricService;

  AsyncSendCaseTaskRunnable(ReplayCaseTransmitService transmitService) {
    this.transmitService = transmitService;
  }

  @Override
  protected void doWithTracedRunning() {
    // TODO: use send time decrease or increase the sendLimiter of replay case
    boolean success = false;
    Throwable t = null;
    try {

      // todo: ignore this in the error counter
      // checkpoint: before sending single case
      if (this.executionStatus.isInterrupted()) {
        transmitService.updateSendResult(caseItem, CaseSendStatusType.EXCEPTION_FAILED);
        return;
      }

      if (this.executionStatus.isCanceled()) {
        transmitService.updateSendResult(caseItem, CaseSendStatusType.CANCELED);
        return;
      }

      MDCTracer.addDetailId(caseItem.getId());
      long caseExecutionStartMillis = System.currentTimeMillis();
      caseItem.setExecutionStartMillis(caseExecutionStartMillis);
      success = this.replaySender.send(caseItem);
      LOGGER.info("async run sender Id: {} , result:{}", caseItem.getId(), success);
      transmitService.updateSendResult(caseItem, success ? CaseSendStatusType.SUCCESS :
          CaseSendStatusType.EXCEPTION_FAILED);
    } catch (Throwable throwable) {
      t = throwable;
      LOGGER.error("async run sender Id: {} , error: {}", caseItem.getId(),
          throwable.getMessage(), throwable);
      transmitService.updateSendResult(caseItem, CaseSendStatusType.EXCEPTION_FAILED);
    } finally {
      groupSentLatch.countDown();
      limiter.release(success);
      caseItem.buildParentErrorMessage(
          t != null ? t.getMessage() : CaseSendStatusType.EXCEPTION_FAILED.name()
      );
      if (!success) {
        metricService.recordCountEvent(LogType.CASE_EXCEPTION_NUMBER.getValue(),
            caseItem.getParent().getPlanId(),
            caseItem.getParent().getAppId(), DEFAULT_COUNT);
      }
      MDCTracer.removeDetailId();
    }
  }
}
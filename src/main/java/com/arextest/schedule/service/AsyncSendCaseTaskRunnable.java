package com.arextest.schedule.service;

import com.arextest.schedule.common.SendSemaphoreLimiter;
import com.arextest.schedule.mdc.AbstractTracedRunnable;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.CaseSendStatusType;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.sender.ReplaySender;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

/**
 * @author jmo
 * @since 2022/2/20
 */
@Slf4j
@Setter
final class AsyncSendCaseTaskRunnable extends AbstractTracedRunnable {
    private transient ReplaySender replaySender;
    private transient ReplayActionCaseItem caseItem;
    private transient CountDownLatch groupSentLatch;
    private transient SendSemaphoreLimiter limiter;

    private transient final ReplayCaseTransmitService transmitService;

    AsyncSendCaseTaskRunnable(ReplayCaseTransmitService transmitService) {
        this.transmitService = transmitService;
    }

    @Override
    protected void doWithTracedRunning() {
        // TODO: use send time decrease or increase the sendLimiter of replay case
        boolean success = false;
        Throwable t = null;
        try {
            MDCTracer.addDetailId(caseItem.getId());
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
            MDCTracer.removeDetailId();
        }
    }
}
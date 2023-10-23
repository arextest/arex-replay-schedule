package com.arextest.schedule.service.noise;

import java.util.concurrent.CountDownLatch;

import com.arextest.schedule.common.SendSemaphoreLimiter;
import com.arextest.schedule.mdc.AbstractTracedRunnable;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.sender.ReplaySender;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by coryhh on 2023/10/17.
 */
@Slf4j
@Setter
public class AsyncNoiseCaseSendTaskRunnable extends AbstractTracedRunnable {

    private transient ReplaySender replaySender;
    private SendSemaphoreLimiter semaphoreLimiter;
    private CountDownLatch countDownLatch;
    private transient ReplayActionCaseItem caseItem;

    public AsyncNoiseCaseSendTaskRunnable(ReplaySender replaySender, SendSemaphoreLimiter semaphoreLimiter,
        CountDownLatch countDownLatch, ReplayActionCaseItem caseItem) {
        this.replaySender = replaySender;
        this.semaphoreLimiter = semaphoreLimiter;
        this.countDownLatch = countDownLatch;
        this.caseItem = caseItem;
    }

    @Override
    protected void doWithTracedRunning() {
        boolean success = false;
        try {
            success = replaySender.send(caseItem);
            LOGGER.info("async run sender Id: {} , result:{}", caseItem.getId(), success);
        } catch (RuntimeException exception) {
            caseItem.setSendErrorMessage(exception.getMessage());
            LOGGER.error("failed to send case for noise analysis: {}", caseItem.getId(), exception);
        } finally {
            semaphoreLimiter.release(success);
            countDownLatch.countDown();
        }
    }
}

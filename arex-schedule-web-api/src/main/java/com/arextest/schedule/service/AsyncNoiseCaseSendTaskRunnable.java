package com.arextest.schedule.service;

import com.arextest.schedule.mdc.AbstractTracedRunnable;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.sender.ReplaySender;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
public class AsyncNoiseCaseSendTaskRunnable extends AbstractTracedRunnable {

    private transient ReplaySender replaySender;
    private transient ReplayActionCaseItem caseItem;

    public AsyncNoiseCaseSendTaskRunnable(ReplaySender replaySender, ReplayActionCaseItem caseItem) {
        this.replaySender = replaySender;
        this.caseItem = caseItem;
    }

    @Override
    protected void doWithTracedRunning() {
        try {
            replaySender.send(caseItem);
        } catch (RuntimeException exception) {
            caseItem.setSendErrorMessage(exception.getMessage());
            LOGGER.error("Failed to send noise case: {}", caseItem, exception);
        }

    }
}

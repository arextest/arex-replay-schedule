package com.arextest.schedule.sender.impl;

import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.sender.ReplaySendResult;
import com.arextest.schedule.sender.ReplaySender;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;


@Slf4j
abstract class AbstractReplaySender implements ReplaySender {
    @Resource
    private MockCachePreLoader mockCachePreLoader;

    protected void bindSendResult(ReplayActionCaseItem caseItem, ReplaySendResult sendResult) {
        caseItem.setTargetResultId(sendResult.getTraceId());
        caseItem.setSendStatus(sendResult.getStatusType().getValue());
    }

    protected void before(String recordId) {
        if (StringUtils.isNotEmpty(recordId)) {
            mockCachePreLoader.fillMockSource(recordId);
        }
    }


    protected void after(ReplayActionCaseItem caseItem) {
        mockCachePreLoader.removeMockSource(caseItem.getRecordId());
    }
}
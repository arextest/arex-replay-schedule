package io.arex.replay.schedule.sender.impl;

import io.arex.replay.schedule.model.ReplayActionCaseItem;
import io.arex.replay.schedule.sender.ReplaySendResult;
import io.arex.replay.schedule.sender.ReplaySender;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;

/**
 * @author jmo
 * @since 2021/9/16
 */
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

package com.arextest.schedule.service;

import com.arextest.schedule.model.DebugRequestItem;
import com.arextest.schedule.sender.ReplaySendResult;
import com.arextest.schedule.sender.ReplaySender;
import com.arextest.schedule.sender.ReplaySenderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author: miaolu
 * @create: 2021-12-08
 **/
@Service
@Slf4j
public class DebugRequestService {
    @Resource
    private ReplaySenderFactory senderFactory;

    public ReplaySendResult debugRequest(DebugRequestItem requestItem) {
        ReplaySender replaySender = senderFactory.findReplaySender(requestItem.getRequestType());
        if (replaySender == null) {
            return ReplaySendResult.failed("requestType is not supported", null);
        }
        return replaySender.send(requestItem);
    }
}
package com.arextest.schedule.sender;

import com.arextest.schedule.model.ReplayActionCaseItem;

import java.util.Map;

/**
 * @author jmo
 * @since 2021/9/16
 */
public interface ReplaySender {
    /**
     * Indicate the instance should be working for the message content type,
     * return true should be used,others skipped
     */
    boolean isSupported(String categoryType);

    /**
     * Try to send the replay case to remote target host
     */
    boolean send(ReplayActionCaseItem caseItem);

    /**
     * Try to send the request message to remote target host
     */
    ReplaySendResult send(SenderParameters senderParameters);

    /**
     * Try to warm up the remote target service before sending
     */
    default boolean activeRemoteService(ReplayActionCaseItem caseItem) {
        return true;
    }
}
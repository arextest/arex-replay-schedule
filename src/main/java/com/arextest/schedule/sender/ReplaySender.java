package com.arextest.schedule.sender;

import com.arextest.schedule.model.ReplayActionCaseItem;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author jmo
 * @since 2021/9/16
 */
public interface ReplaySender extends InitializingBean {
    /**
     * Indicate the instance should be working for the message content type,
     * return true should be used,others skipped
     */
    @Deprecated
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
     * Try to prepare the replay case remote dependency such as resume config files
     */
    boolean prepareRemoteDependency(ReplayActionCaseItem caseItem);

    /**
     * Try to warm up the remote target service before sending
     */
    default boolean activeRemoteService(ReplayActionCaseItem caseItem) {
        return true;
    }
}
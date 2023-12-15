package com.arextest.schedule.api.listener;

import com.arextest.schedule.model.ReplayActionCaseItem;

/**
 * Replay result listener, execute in annotation @Order
 */
public interface ReplayResultListener {
    /**
     * notify listener to execute
     */
    void notify(ReplayActionCaseItem caseItem);
}

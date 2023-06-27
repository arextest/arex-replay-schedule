package com.arextest.schedule.comparer;

import com.arextest.schedule.model.ReplayActionCaseItem;

/**
 * @author jmo
 * @since 2021/9/16
 */
public interface ReplayResultComparer {
    boolean compare(ReplayActionCaseItem caseItem, boolean useReplayId);
}
package com.arextest.replay.schedule.comparer;

import com.arextest.replay.schedule.model.ReplayActionCaseItem;

/**
 * @author jmo
 * @since 2021/9/16
 */
public interface ReplayResultComparer {
    boolean compare(ReplayActionCaseItem caseItem);
}

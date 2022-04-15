package io.arex.replay.schedule.comparer;

import io.arex.replay.schedule.model.ReplayActionCaseItem;

/**
 * @author jmo
 * @since 2021/9/16
 */
public interface ReplayResultComparer {
    boolean compare(ReplayActionCaseItem caseItem);
}

package io.arex.replay.schedule.progress;

import io.arex.replay.schedule.model.ReplayActionCaseItem;
import io.arex.replay.schedule.model.ReplayPlan;

/**
 * @author jmo
 * @since 2021/10/11
 */
public interface ProgressTracer {
    void initTotal(ReplayPlan replayPlan);

    void finishOne(ReplayActionCaseItem caseItem);

    double finishPercent(long planId);

    long lastUpdateTime(long planId);
}

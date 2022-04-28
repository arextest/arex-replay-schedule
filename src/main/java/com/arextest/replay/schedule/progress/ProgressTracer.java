package com.arextest.replay.schedule.progress;

import com.arextest.replay.schedule.model.ReplayActionCaseItem;
import com.arextest.replay.schedule.model.ReplayPlan;

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

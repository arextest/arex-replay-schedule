package com.arextest.schedule.progress;

import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;

/**
 * @author jmo
 * @since 2021/10/11
 */
public interface ProgressTracer {
    void initTotal(ReplayPlan replayPlan);

    void finishOne(ReplayActionCaseItem caseItem);

    void finishCaseByAction(ReplayActionItem actionItem);

    void finishCaseByPlan(ReplayPlan replayPlan);

    double finishPercent(String planId);

    long lastUpdateTime(String planId);
}
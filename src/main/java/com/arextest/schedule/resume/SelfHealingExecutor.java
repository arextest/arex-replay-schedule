package com.arextest.schedule.resume;

import com.arextest.schedule.model.ReplayPlan;

import java.time.Duration;
import java.util.List;

/**
 * Created by Qzmo on 2023/5/24
 */
public interface SelfHealingExecutor {
    List<ReplayPlan> queryTimeoutPlan(Duration offsetDuration, Duration maxDuration);

    /**
     * Resume single replay plan
     */
    void doResume(ReplayPlan replayPlan);
}

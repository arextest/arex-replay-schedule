package com.arextest.schedule.service;

import com.arextest.schedule.model.ReplayPlan;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * created by xinyuan_wang on 2023/1/12
 */
@Slf4j
public final class PlanFinishService {

    private final List<PlanFinishListener> planFinishListeners;

    public PlanFinishService(List<PlanFinishListener> planFinishListeners) {
        this.planFinishListeners = planFinishListeners;
    }

    public void onPlanFinishEvent(ReplayPlan replayPlan) {
        if (CollectionUtils.isEmpty(this.planFinishListeners)) {
            return;
        }
        for (PlanFinishListener planFinishListener : this.planFinishListeners) {
            planFinishListener.planFinishAction(replayPlan);
        }
    }

}
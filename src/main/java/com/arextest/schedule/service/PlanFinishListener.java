package com.arextest.schedule.service;


import com.arextest.schedule.model.ReplayPlan;

/**
 * @author jmo
 * @since 2022/1/28
 */
public interface PlanFinishListener {

    void planFinishAction(ReplayPlan replayPlan);

}
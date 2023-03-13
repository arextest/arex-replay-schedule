package com.arextest.schedule.service;


import com.arextest.schedule.model.ReplayPlan;

/**
 * @author jmo
 * @since 2022/2/19
 */
public interface ScheduleConfigurationListener {

    void buildReplayConfigurationEvent(ReplayPlan replayPlan);

}
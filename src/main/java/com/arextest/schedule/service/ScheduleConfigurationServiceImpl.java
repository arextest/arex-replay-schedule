package com.arextest.schedule.service;

import com.arextest.schedule.model.ReplayPlan;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.arextest.schedule.common.CommonConstant.OPERATION_MAX_CASE_COUNT;

/**
 * created by xinyuan_wang on 2023/3/13
 */
@Component
public class ScheduleConfigurationServiceImpl implements ScheduleConfigurationListener {
    @Resource
    private ConfigurationService configurationService;

    @Override
    public void buildReplayConfigurationEvent(ReplayPlan replayPlan) {
        ConfigurationService.ScheduleConfiguration schedule = configurationService.schedule(replayPlan.getAppId());
        if (schedule != null) {
            replayPlan.setReplaySendMaxQps(schedule.getSendMaxQps());
            replayPlan.setCaseCountLimit(schedule.getCaseCountLimit() == null ? OPERATION_MAX_CASE_COUNT : schedule.getCaseCountLimit());
        } else {
            replayPlan.setCaseCountLimit(OPERATION_MAX_CASE_COUNT);
        }
    }
}

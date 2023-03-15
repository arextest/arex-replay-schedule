package com.arextest.schedule.service;

import com.arextest.schedule.model.ReplayPlan;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * created by xinyuan_wang on 2023/1/12
 */
@Slf4j
@Service
public final class ScheduleConfigurationService {

    private final List<ScheduleConfigurationListener> scheduleConfigurationListeners;

    public ScheduleConfigurationService(List<ScheduleConfigurationListener> scheduleConfigurationListeners) {
        this.scheduleConfigurationListeners = scheduleConfigurationListeners;
    }

    public void buildReplayConfiguration(ReplayPlan replayPlan) {
        if (CollectionUtils.isEmpty(this.scheduleConfigurationListeners)) {
            return;
        }
        for (ScheduleConfigurationListener provider : this.scheduleConfigurationListeners) {
            provider.buildReplayConfigurationEvent(replayPlan);
            break;
        }
    }


}
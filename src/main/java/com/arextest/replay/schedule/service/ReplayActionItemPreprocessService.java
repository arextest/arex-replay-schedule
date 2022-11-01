package com.arextest.replay.schedule.service;

import com.arextest.replay.schedule.model.ReplayActionItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by rchen9 on 2022/10/31.
 */
@Slf4j
@Component
public class ReplayActionItemPreprocessService {

    @Resource
    private ConfigurationService configurationService;

    @Resource
    private ObjectMapper objectMapper;

    public void addExclusionOperation(List<ReplayActionItem> replayActionItemList, String appId) {
        if (CollectionUtils.isEmpty(replayActionItemList) || StringUtils.isEmpty(appId)) {
            return;
        }
        ConfigurationService.ScheduleConfiguration schedule = configurationService.schedule(appId);
        if (schedule == null || MapUtils.isEmpty(schedule.getExcludeOperationMap())) {
            return;
        }
        try {
            String exclusionMapString = objectMapper.writeValueAsString(schedule.getExcludeOperationMap());
            replayActionItemList.forEach(item -> {
                item.setExclusionOperationConfig(exclusionMapString);
            });
        } catch (JsonProcessingException e) {
            LOGGER.warn("ReplayActionItemPreprocessService.addHeaders failed,message:{}", e.getMessage());
        }

    }
}

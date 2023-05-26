package com.arextest.schedule.service;

import com.arextest.schedule.model.ReplayActionItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

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

    public void filterActionItemAndAddExclusionOperation(List<ReplayActionItem> replayActionItemList, String appId) {
        if (CollectionUtils.isEmpty(replayActionItemList) || StringUtils.isEmpty(appId)) {
            return;
        }
        ConfigurationService.ScheduleConfiguration schedule = configurationService.schedule(appId);
        if (schedule == null) {
            return;
        }
        if (CollectionUtils.isNotEmpty(schedule.getExcludeServiceOperationSet())) {
            // 根据ExcludeServiceOperationSet filter replayActionItemList by ExcludeServiceOperationSet
            Iterator<ReplayActionItem> iterator = replayActionItemList.iterator();
            while (iterator.hasNext()) {
                if (schedule.getExcludeServiceOperationSet().contains(iterator.next().getOperationName())) {
                    iterator.remove();
                }
            }
        }

        if (MapUtils.isEmpty(schedule.getExcludeOperationMap())) {
            return;
        }
        try {
            String exclusionMapString = objectMapper.writeValueAsString(schedule.getExcludeOperationMap());
            for (ReplayActionItem replayActionItem : replayActionItemList) {
                replayActionItem.setExclusionOperationConfig(exclusionMapString);
            }
        } catch (JsonProcessingException e) {
            LOGGER.warn("ReplayActionItemPreprocessService.addHeaders failed,message:{}", e.getMessage());
        }

    }
}
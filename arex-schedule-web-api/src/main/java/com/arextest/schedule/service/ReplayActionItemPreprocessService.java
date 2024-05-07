package com.arextest.schedule.service;

import com.arextest.schedule.model.ReplayActionItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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

  private static final String SEPARATOR_STAR = "*";

  @Resource
  private ConfigurationService configurationService;

  @Resource
  private ObjectMapper objectMapper;

  public List<String> filterActionItem(List<ReplayActionItem> replayActionItemList, String appId) {
    if (CollectionUtils.isEmpty(replayActionItemList) || StringUtils.isEmpty(appId)) {
      return null;
    }
    ConfigurationService.ScheduleConfiguration schedule = configurationService.schedule(appId);
    if (schedule == null) {
      return null;
    }
    List<String> excludedActionIds = filter(replayActionItemList,
        schedule.getIncludeServiceOperationSet(),
        schedule.getExcludeServiceOperationSet());

    if (MapUtils.isNotEmpty(schedule.getExcludeOperationMap())) {
      try {
        String exclusionMapString = objectMapper.writeValueAsString(
            schedule.getExcludeOperationMap());
        for (ReplayActionItem replayActionItem : replayActionItemList) {
          replayActionItem.setExclusionOperationConfig(exclusionMapString);
        }
      } catch (JsonProcessingException e) {
        LOGGER.warn("ReplayActionItemPreprocessService.addHeaders failed,message:{}",
            e.getMessage());
      }
    }
    return excludedActionIds;
  }

  private List<String> filter(List<ReplayActionItem> replayActionItemList,
      Set<String> includeOperations,
      Set<String> excludeOperations) {

    List<String> excludedActionIds = new ArrayList<>();

    if (CollectionUtils.isEmpty(replayActionItemList)) {
      return excludedActionIds;
    }
    Iterator<ReplayActionItem> iterator = replayActionItemList.iterator();
    if (CollectionUtils.isNotEmpty(includeOperations)) {
      while (iterator.hasNext()) {
        ReplayActionItem replayActionItem = iterator.next();
        if (!isMatch(replayActionItem.getOperationName(), includeOperations)) {
          iterator.remove();
          excludedActionIds.add(replayActionItem.getId());
        }
      }
      return excludedActionIds;
    }
    if (CollectionUtils.isNotEmpty(excludeOperations)) {
      while (iterator.hasNext()) {
        ReplayActionItem replayActionItem = iterator.next();
        if (isMatch(replayActionItem.getOperationName(), excludeOperations)) {
          iterator.remove();
          excludedActionIds.add(replayActionItem.getId());
        }
      }
    }
    return excludedActionIds;
  }

  private boolean isMatch(String targetName, Set<String> operations) {
    if (StringUtils.isEmpty(targetName)) {
      return false;
    }
    for (String operation : operations) {
      if (operation.equalsIgnoreCase(targetName)) {
        return true;
      }

      if (operation.startsWith(SEPARATOR_STAR) &&
          targetName.endsWith(operation.substring(1))) {
        return true;
      }
      if (operation.endsWith(SEPARATOR_STAR) &&
          targetName.startsWith(operation.substring(0, operation.length() - 1))) {
        return true;
      }
    }
    return false;
  }
}
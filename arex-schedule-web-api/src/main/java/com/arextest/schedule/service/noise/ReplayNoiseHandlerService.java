package com.arextest.schedule.service.noise;

import com.arextest.common.model.response.GenericResponseType;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.dao.mongodb.ReplayNoiseRepository;
import com.arextest.schedule.dao.mongodb.util.MongoHelper;
import com.arextest.schedule.model.converter.ReplayNoiseConverter;
import com.arextest.schedule.model.dao.mongodb.ReplayNoiseCollection;
import com.arextest.schedule.model.dao.mongodb.ReplayNoiseCollection.ReplayNoiseItemDao;
import com.arextest.schedule.model.noiseidentify.ExcludeNoiseRequestType;
import com.arextest.schedule.model.noiseidentify.QueryNoiseResponseType;
import com.arextest.schedule.model.noiseidentify.QueryNoiseResponseType.InterfaceNoiseItem;
import com.arextest.schedule.model.noiseidentify.QueryNoiseResponseType.NoiseItem;
import com.arextest.schedule.model.noiseidentify.ReplayNoiseDto;
import com.arextest.schedule.model.noiseidentify.ReplayNoiseItemDto;
import com.arextest.schedule.model.noiseidentify.ReplayNoiseStatus;
import com.arextest.schedule.model.noiseidentify.UpdateNoiseItem;
import com.arextest.schedule.utils.ListUtils;
import com.arextest.schedule.utils.MapUtils;
import com.arextest.web.model.contract.contracts.config.replay.ComparisonExclusionsConfiguration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReplayNoiseHandlerService {

  @Resource
  private ReplayNoiseRepository replayNoiseRepository;

  @Resource
  private HttpWepServiceApiClient httpWepServiceApiClient;

  @Value("${arex.api.config.addExclusion.url}")
  private String addExclusionConfigUrl;

  public QueryNoiseResponseType queryNoise(String planId, String planItemId) {

    List<InterfaceNoiseItem> appNoiseList = new ArrayList<>();

    List<ReplayNoiseDto> replayNoiseDtoList = replayNoiseRepository.queryReplayNoise(planId,
        planItemId);
    Map<String, List<ReplayNoiseDto>> interfaceReplayNoiseMap =
        replayNoiseDtoList.stream().collect(Collectors.groupingBy(ReplayNoiseDto::getOperationId));
    for (Map.Entry<String, List<ReplayNoiseDto>> interfaceReplayNoiseEntry : interfaceReplayNoiseMap.entrySet()) {
      String operationId = interfaceReplayNoiseEntry.getKey();
      List<ReplayNoiseDto> interfaceReplayNoise = interfaceReplayNoiseEntry.getValue();
      if (CollectionUtils.isEmpty(interfaceReplayNoise)) {
        continue;
      }

      List<QueryNoiseResponseType.MockerNoiseItem> randomNoiseList = new ArrayList<>();
      List<QueryNoiseResponseType.MockerNoiseItem> disorderedArrayNoise = new ArrayList<>();

      for (ReplayNoiseDto replayNoiseDto : interfaceReplayNoise) {
        String categoryName = replayNoiseDto.getCategoryName();
        MockCategoryType mockCategoryType = MockCategoryType.create(categoryName);

        Map<String, ReplayNoiseItemDto> mayIgnoreItems = replayNoiseDto.getMayIgnoreItems();
        if (MapUtils.isNotEmpty(mayIgnoreItems)) {
          Set<Entry<String, ReplayNoiseItemDto>> entries = mayIgnoreItems.entrySet();

          List<QueryNoiseResponseType.NoiseItem> randomNoiseItems = new ArrayList<>(entries.size());
          for (Entry<String, ReplayNoiseItemDto> itemDtoEntry : entries) {
            String identifier = itemDtoEntry.getKey();
            ReplayNoiseItemDto value = itemDtoEntry.getValue();
            NoiseItem noiseItem = ReplayNoiseConverter.INSTANCE.toNoiseItem(value);
            noiseItem.setIdentifier(identifier);
            randomNoiseItems.add(noiseItem);
          }
          if (CollectionUtils.isNotEmpty(randomNoiseItems)) {
            randomNoiseList.add(new QueryNoiseResponseType.MockerNoiseItem(mockCategoryType,
                replayNoiseDto.getOperationName(), categoryName, randomNoiseItems));
          }
        }

        Map<String, ReplayNoiseItemDto> mayDisorderItems = replayNoiseDto.getMayDisorderItems();
        if (MapUtils.isNotEmpty(mayDisorderItems)) {
          Set<Entry<String, ReplayNoiseItemDto>> entries = mayDisorderItems.entrySet();
          List<QueryNoiseResponseType.NoiseItem> disorderedArrayNoiseItems = new ArrayList<>(
              entries.size());
          for (Entry<String, ReplayNoiseItemDto> itemDtoEntry : entries) {

            String identifier = itemDtoEntry.getKey();
            ReplayNoiseItemDto value = itemDtoEntry.getValue();
            // XXX: simple array filter, improve: more accurate recommendations
            if (value.getPathCount() < 2) {
              continue;
            }
            NoiseItem noiseItem = ReplayNoiseConverter.INSTANCE.toNoiseItem(value);
            noiseItem.setIdentifier(identifier);
          }
          if (CollectionUtils.isNotEmpty(disorderedArrayNoiseItems)) {
            disorderedArrayNoise.add(new QueryNoiseResponseType.MockerNoiseItem(mockCategoryType,
                replayNoiseDto.getOperationName(), categoryName, disorderedArrayNoiseItems));
          }
        }
      }

      if (CollectionUtils.isNotEmpty(randomNoiseList) || CollectionUtils.isNotEmpty(
          disorderedArrayNoise)) {
        appNoiseList.add(
            new QueryNoiseResponseType.InterfaceNoiseItem(operationId, randomNoiseList,
                disorderedArrayNoise));
      }
    }
    QueryNoiseResponseType result = new QueryNoiseResponseType();
    result.setInterfaceNoiseItemList(appNoiseList);
    return result;
  }


  public boolean excludeNoise(ExcludeNoiseRequestType excludeNoiseRequestType) {

    List<UpdateNoiseItem> updateNoiseItemList = new ArrayList<>();
    List<ComparisonExclusionsConfiguration> exclusionConfigs = new ArrayList<>();

    List<InterfaceNoiseItem> interfaceNoiseItemList = Optional.ofNullable(
        excludeNoiseRequestType.getInterfaceNoiseItemList()).orElse(
        Collections.emptyList());
    for (InterfaceNoiseItem interfaceNoiseItem : interfaceNoiseItemList) {
      String operationId = interfaceNoiseItem.getOperationId();
      List<QueryNoiseResponseType.MockerNoiseItem> randomNoiseList = Optional.ofNullable(
          interfaceNoiseItem.getRandomNoise()).orElse(Collections.emptyList());
      for (QueryNoiseResponseType.MockerNoiseItem mockerNoiseItem : randomNoiseList) {
        UpdateNoiseItem updateNoiseItem = new UpdateNoiseItem();
        updateNoiseItem.setQueryFields(
            MapUtils.createMap(
                ReplayNoiseCollection.FIELD_PLAN_ID,
                excludeNoiseRequestType.getPlanId(),
                ReplayNoiseCollection.FIELD_OPERATION_ID,
                operationId,
                ReplayNoiseCollection.FIELD_CATEGORY_NAME,
                mockerNoiseItem.getOperationType()
            )
        );

        Map<String, Object> updateFields = new HashMap<>();
        List<QueryNoiseResponseType.NoiseItem> noiseItemList = Optional.ofNullable(
            mockerNoiseItem.getNoiseItemList()).orElse(Collections.emptyList());
        for (QueryNoiseResponseType.NoiseItem noiseItem : noiseItemList) {
          String identifier = noiseItem.getIdentifier();
          // add exclude noise item
          updateFields.put(MongoHelper.appendDot(ReplayNoiseCollection.FIELD_MAY_IGNORE_ITEMS,
              identifier, ReplayNoiseItemDao.FIELD_STATUS), ReplayNoiseStatus.STATUS_EXCLUDE);

          // add exclusions to config
          ComparisonExclusionsConfiguration exclusionConfig = new ComparisonExclusionsConfiguration();
          exclusionConfig.setAppId(excludeNoiseRequestType.getAppId());
          exclusionConfig.setOperationId(operationId);
          if (!mockerNoiseItem.getMockCategoryType().isEntryPoint()) {
            exclusionConfig.setOperationType(mockerNoiseItem.getOperationType());
            exclusionConfig.setOperationName(mockerNoiseItem.getOperationName());
          }
          List<String> excludePath = ListUtils.getFuzzyPathStrList(noiseItem.getNodeEntity());
          if (excludePath != null) {
            exclusionConfig.setExclusions(excludePath);
            exclusionConfigs.add(exclusionConfig);
          }
        }
        updateNoiseItem.setUpdateFields(updateFields);
        updateNoiseItemList.add(updateNoiseItem);
      }
    }

    boolean result = replayNoiseRepository.updateReplayNoiseStatus(updateNoiseItemList);
    if (!result) {
      return false;
    }

    if (CollectionUtils.isNotEmpty(exclusionConfigs)) {
      GenericResponseType response = httpWepServiceApiClient.jsonPost(addExclusionConfigUrl,
          exclusionConfigs,
          GenericResponseType.class);
      if (response == null || Objects.equals(response.getBody(), false)) {
        LOGGER.error("add exclusion config failed, response: {}", response);
        return false;
      }
    }
    return true;
  }

}

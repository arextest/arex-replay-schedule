package com.arextest.schedule.comparer.impl;

import com.arextest.model.replay.CompareRelationResult;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.replay.QueryReplayResultRequestType;
import com.arextest.model.replay.QueryReplayResultResponseType;
import com.arextest.model.replay.holder.ListResultHolder;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.comparer.CategoryComparisonHolder;
import com.arextest.schedule.comparer.CategoryComparisonHolder.CompareResultItem;
import com.arextest.schedule.comparer.CompareItem;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.serialization.ZstdJacksonSerializer;
import com.arextest.schedule.service.MetricService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public final class PrepareCompareSourceRemoteLoader {

  private static final int INDEX_NOT_FOUND = -1;
  @Resource
  MetricService metricService;
  @Value("${arex.storage.replayResult.url}")
  private String replayResultUrl;
  @Resource
  private HttpWepServiceApiClient httpWepServiceApiClient;
  @Resource
  private ZstdJacksonSerializer zstdJacksonSerializer;
  @Resource
  private PrepareCompareItemBuilder prepareCompareItemBuilder;

  public List<CategoryComparisonHolder> buildWaitCompareList(ReplayActionCaseItem caseItem,
      boolean useReplayId) {
    String targetResultId = null;
    String sourceResultId = null;
    if (useReplayId) {
      targetResultId = caseItem.getTargetResultId();
      sourceResultId = caseItem.getSourceResultId();
    }
    final String recordId = caseItem.getRecordId();

    if (StringUtils.isNotBlank(sourceResultId)) {

      List<CategoryComparisonHolder> sourceResponse = this.getReplayResult(recordId,
          sourceResultId);
      List<CategoryComparisonHolder> targetResponse = this.getReplayResult(recordId,
          targetResultId);
      if (CollectionUtils.isEmpty(sourceResponse) || CollectionUtils.isEmpty(targetResponse)) {
        LOGGER.warn(
            "replay recordId:{} invalid response,source replayId:{} size:{},target replayId:{} size:{}",
            recordId, sourceResultId, sourceResponse.size(), targetResultId, targetResponse.size());
        return Collections.emptyList();
      }
      return buildWaitCompareList(sourceResponse, targetResponse);
    }
    List<CategoryComparisonHolder> replayResult = this.getReplayResult(recordId, targetResultId);
    // todo record the QMessage replay log, which will be optimized for removal later.
    metricService.recordTraceIdEvent(caseItem, replayResult);
    return replayResult;
  }

  // TODO: In the scenario where the operation is empty, there is a problem of redundant returns in the record.
  public List<CategoryComparisonHolder> getReplayResult(String replayId, String resultId) {
    QueryReplayResultResponseType responseType = remoteLoad(replayId, resultId);
    return decodeResult(responseType);
  }

  private QueryReplayResultResponseType remoteLoad(String replayId, String resultId) {
    QueryReplayResultRequestType resultRequest = new QueryReplayResultRequestType();
    resultRequest.setRecordId(replayId);
    resultRequest.setReplayResultId(resultId);
    return httpWepServiceApiClient.retryZstdJsonPost(replayResultUrl,
        resultRequest, QueryReplayResultResponseType.class);
  }

  /**
   * Compatible logic.
   * The subsequent agent will match the packets and does not need to be processed in the schedule.
   * @param replayResultResponseType
   * @return
   */
  private List<CategoryComparisonHolder> decodeResult(
      QueryReplayResultResponseType replayResultResponseType) {
    if (replayResultResponseType == null ||
        replayResultResponseType.getResponseStatusType() == null ||
        replayResultResponseType.getResponseStatusType().hasError() ||
        Boolean.TRUE.equals(replayResultResponseType.getInvalidResult())) {
      LOGGER.warn("failed to get replay result because of invalid case");
      return Collections.emptyList();
    }

    // Use needMatch to determine whether to adopt new logic
    return Boolean.TRUE.equals(replayResultResponseType.getNeedMatch()) ?
        processMatchNeeded(replayResultResponseType) : processMatchNotNeeded(replayResultResponseType);
  }

  /**
   * Processing of messages that require matching relationships that need to be compared
   * @param replayResultResponseType
   * @return
   */
  private List<CategoryComparisonHolder> processMatchNeeded(QueryReplayResultResponseType replayResultResponseType) {
    List<ListResultHolder> resultHolderList = replayResultResponseType.getResultHolderList();
    if (CollectionUtils.isEmpty(resultHolderList)) {
      LOGGER.warn("query replay result has empty size");
      return Collections.emptyList();
    }

    List<CategoryComparisonHolder> decodedListResult = new ArrayList<>(resultHolderList.size());
    for (ListResultHolder stringListResultHolder : resultHolderList) {
      MockCategoryType categoryType = stringListResultHolder.getCategoryType();
      if (categoryType == null || (categoryType.isSkipComparison() &&
          !MockCategoryType.Q_MESSAGE_CONSUMER.getName().equalsIgnoreCase(categoryType.getName()))) {
        continue;
      }

      CategoryComparisonHolder resultHolder = new CategoryComparisonHolder();
      resultHolder.setCategoryName(categoryType.getName());
      decodedListResult.add(resultHolder);

      List<CompareItem> recordList = zstdDeserialize(stringListResultHolder.getRecord());
      List<CompareItem> replayResultList = zstdDeserialize(stringListResultHolder.getReplayResult());

      if (categoryType.isEntryPoint() && (CollectionUtils.isEmpty(recordList) || CollectionUtils.isEmpty(replayResultList))) {
        // call missing or new call
        return Collections.emptyList();
      }

      resultHolder.setRecord(recordList);
      resultHolder.setReplayResult(replayResultList);
      resultHolder.setNeedMatch(true);
    }
    return decodedListResult;
  }

  /**
   * Processing of packets that do not require matching
   * @param replayResultResponseType
   * @return
   */
  private List<CategoryComparisonHolder> processMatchNotNeeded(QueryReplayResultResponseType replayResultResponseType) {
    List<CompareRelationResult> replayResults = replayResultResponseType.getReplayResults();
    if (CollectionUtils.isEmpty(replayResults)) {
      LOGGER.warn("query replay result has empty size");
      return Collections.emptyList();
    }

    List<CategoryComparisonHolder> decodedListResult = new ArrayList<>(replayResults.size());
    for (CompareRelationResult result : replayResults) {
      MockCategoryType categoryType = result.getCategoryType();
      if (categoryType == null || (!categoryType.isEntryPoint() && categoryType.isSkipComparison())) {
        continue;
      }

      if (categoryType.isEntryPoint() && !categoryType.isSkipComparison() &&
          (StringUtils.isEmpty(result.getRecordMessage()) || StringUtils.isEmpty(result.getReplayMessage()))) {
        // The main category is missing
        return Collections.emptyList();
      }

      CategoryComparisonHolder resultHolder = new CategoryComparisonHolder();
      resultHolder.setCategoryName(categoryType.getName());

      CompareItem recordCompareItem = prepareCompareItemBuilder.build(result, true);
      CompareItem replayCompareItem = prepareCompareItemBuilder.build(result, false);

      resultHolder.setCompareResultItem(new CompareResultItem(recordCompareItem, replayCompareItem));
      resultHolder.setNeedMatch(false);
      decodedListResult.add(resultHolder);
    }
    return decodedListResult;
  }

  private List<CompareItem> zstdDeserialize(List<String> base64List) {
    if (CollectionUtils.isEmpty(base64List)) {
      return Collections.emptyList();
    }
    List<CompareItem> decodedResult = new ArrayList<>(base64List.size());
    for (int i = 0; i < base64List.size(); i++) {
      String base64 = base64List.get(i);
      AREXMocker source = zstdJacksonSerializer.deserialize(base64, AREXMocker.class);
      if (source == null) {
        continue;
      }
      CompareItem item = prepareCompareItemBuilder.build(source);
      if (item != null) {
        decodedResult.add(item);
      }
    }
    return decodedResult;
  }

  private List<CategoryComparisonHolder> buildWaitCompareList(
      List<CategoryComparisonHolder> sourceResult,
      List<CategoryComparisonHolder> targetResultList) {
    for (CategoryComparisonHolder sourceResultHolder : sourceResult) {
      int targetIndex = findResultByCategory(targetResultList,
          sourceResultHolder.getCategoryName());
      sourceResultHolder.setRecord(sourceResultHolder.getReplayResult());
      if (targetIndex == INDEX_NOT_FOUND) {
        continue;
      }
      CategoryComparisonHolder targetResult = targetResultList.get(targetIndex);
      sourceResultHolder.setReplayResult(targetResult.getReplayResult());
      targetResultList.remove(targetIndex);
    }
    if (CollectionUtils.isNotEmpty(targetResultList)) {
      for (CategoryComparisonHolder resultHolder : targetResultList) {
        resultHolder.setRecord(Collections.emptyList());
        sourceResult.add(resultHolder);
      }
    }
    return sourceResult;
  }

  private int findResultByCategory(List<CategoryComparisonHolder> source, String category) {
    for (int i = 0; i < source.size(); i++) {
      CategoryComparisonHolder resultHolder = source.get(i);
      if (StringUtils.equals(resultHolder.getCategoryName(), category)) {
        return i;
      }
    }
    return INDEX_NOT_FOUND;
  }
}
package com.arextest.schedule.comparer.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.replay.QueryReplayResultRequestType;
import com.arextest.model.replay.QueryReplayResultResponseType;
import com.arextest.model.replay.holder.ListResultHolder;
import com.arextest.model.response.ResponseStatusType;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.comparer.CategoryComparisonHolder;
import com.arextest.schedule.comparer.CompareItem;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.serialization.ZstdJacksonSerializer;
import com.arextest.schedule.service.MetricService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public final class PrepareCompareSourceRemoteLoader {
    @Value("${arex.storage.replayResult.url}")
    private String replayResultUrl;
    @Resource
    private HttpWepServiceApiClient httpWepServiceApiClient;
    @Resource
    private ZstdJacksonSerializer zstdJacksonSerializer;
    @Resource
    private PrepareCompareItemBuilder prepareCompareItemBuilder;

    @Resource
    MetricService metricService;

    private static final int INDEX_NOT_FOUND = -1;

    public List<CategoryComparisonHolder> buildWaitCompareList(ReplayActionCaseItem caseItem, boolean useReplayId) {
        String targetResultId = null;
        String sourceResultId = null;
        if (useReplayId) {
            targetResultId = caseItem.getTargetResultId();
            sourceResultId = caseItem.getSourceResultId();
        }
        final String recordId = caseItem.getRecordId();

        if (StringUtils.isNotBlank(sourceResultId)) {

            List<CategoryComparisonHolder> sourceResponse = this.getReplayResult(recordId, sourceResultId);
            List<CategoryComparisonHolder> targetResponse = this.getReplayResult(recordId, targetResultId);
            if (CollectionUtils.isEmpty(sourceResponse) || CollectionUtils.isEmpty(targetResponse)) {
                LOGGER.warn("replay recordId:{} invalid response,source replayId:{} size:{},target replayId:{} size:{}",
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
        return httpWepServiceApiClient.retryJsonPost(replayResultUrl, resultRequest,
            QueryReplayResultResponseType.class);
    }

    private List<CategoryComparisonHolder> decodeResult(QueryReplayResultResponseType replayResultResponseType) {
        if (replayResultResponseType == null) {
            return Collections.emptyList();
        }
        ResponseStatusType responseStatusType = replayResultResponseType.getResponseStatusType();
        if (responseStatusType == null) {
            return Collections.emptyList();
        }
        if (responseStatusType.hasError()) {
            LOGGER.warn("query replay result has error response : {}", responseStatusType);
            return Collections.emptyList();
        }
        List<ListResultHolder> resultHolderList = replayResultResponseType.getResultHolderList();
        if (CollectionUtils.isEmpty(resultHolderList)) {
            LOGGER.warn("query replay result has empty size");
            return Collections.emptyList();
        }
        List<CategoryComparisonHolder> decodedListResult = new ArrayList<>(resultHolderList.size());
        MockCategoryType categoryType;

        for (int i = 0; i < resultHolderList.size(); i++) {
            ListResultHolder stringListResultHolder = resultHolderList.get(i);
            categoryType = stringListResultHolder.getCategoryType();
            if (categoryType == null || categoryType.isSkipComparison()) {
                continue;
            }

            CategoryComparisonHolder resultHolder = new CategoryComparisonHolder();
            resultHolder.setCategoryName(categoryType.getName());
            decodedListResult.add(resultHolder);
            List<CompareItem> recordList = zstdDeserialize(stringListResultHolder.getRecord());
            resultHolder.setRecord(recordList);
            List<CompareItem> replayResultList = zstdDeserialize(stringListResultHolder.getReplayResult());
            resultHolder.setReplayResult(replayResultList);
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

    private List<CategoryComparisonHolder> buildWaitCompareList(List<CategoryComparisonHolder> sourceResult,
        List<CategoryComparisonHolder> targetResultList) {
        for (CategoryComparisonHolder sourceResultHolder : sourceResult) {
            int targetIndex = findResultByCategory(targetResultList, sourceResultHolder.getCategoryName());
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
package com.arextest.schedule.comparer.impl;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.replay.QueryReplayResultRequestType;
import com.arextest.model.replay.QueryReplayResultResponseType;
import com.arextest.model.replay.holder.ListResultHolder;
import com.arextest.model.response.ResponseStatusType;
import com.arextest.schedule.comparer.CategoryComparisonHolder;
import com.arextest.schedule.serialization.ZstdJacksonSerializer;

import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.comparer.CompareItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


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

    public List<CategoryComparisonHolder> getReplayResult(String replayId, String resultId) {
        QueryReplayResultResponseType responseType = remoteLoad(replayId, resultId);
        return decodeResult(responseType);
    }

    private QueryReplayResultResponseType remoteLoad(String replayId, String resultId) {
        QueryReplayResultRequestType resultRequest = new QueryReplayResultRequestType();
        resultRequest.setRecordId(replayId);
        resultRequest.setReplayResultId(resultId);
        return httpWepServiceApiClient.retryJsonPost(replayResultUrl, resultRequest, QueryReplayResultResponseType.class);
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
}
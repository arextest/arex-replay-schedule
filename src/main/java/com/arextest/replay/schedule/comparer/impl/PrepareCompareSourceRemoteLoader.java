package com.arextest.replay.schedule.comparer.impl;

import com.arextest.replay.schedule.serialization.ZstdJacksonSerializer;
import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.header.ResponseStatusType;
import com.arextest.storage.model.replay.QueryReplayResultRequestType;
import com.arextest.storage.model.replay.QueryReplayResultResponseType;
import com.arextest.storage.model.replay.holder.ListResultHolder;
import com.arextest.replay.schedule.client.HttpWepServiceApiClient;
import com.arextest.replay.schedule.comparer.CompareItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * https://www.cnblogs.com/0201zcr/p/6262762.html
 *
 * @author jmo
 * @since 2021/11/22
 */
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

    public List<ListResultHolder<CompareItem>> getReplayResult(String replayId, String resultId) {
        QueryReplayResultResponseType responseType = remoteLoad(replayId, resultId);
        return decodeResult(responseType);
    }

    private QueryReplayResultResponseType remoteLoad(String replayId, String resultId) {
        QueryReplayResultRequestType resultRequest = new QueryReplayResultRequestType();
        resultRequest.setRecordId(replayId);
        resultRequest.setReplayResultId(resultId);
        return httpWepServiceApiClient.jsonPost(replayResultUrl, resultRequest, QueryReplayResultResponseType.class);
    }

    private List<ListResultHolder<CompareItem>> decodeResult(QueryReplayResultResponseType replayResultResponseType) {
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
        List<ListResultHolder<String>> resultHolderList = replayResultResponseType.getResultHolderList();
        if (CollectionUtils.isEmpty(resultHolderList)) {
            LOGGER.warn("query replay result has empty size");
            return Collections.emptyList();
        }
        List<ListResultHolder<CompareItem>> decodedListResult = new ArrayList<>(resultHolderList.size());
        MockCategoryType categoryType;
        Class<?> mockImplClass;
        for (int i = 0; i < resultHolderList.size(); i++) {
            ListResultHolder<String> stringListResultHolder = resultHolderList.get(i);
            categoryType = MockCategoryType.of(stringListResultHolder.getCategoryName());
            if (categoryType == null) {
                continue;
            }
            mockImplClass = categoryType.getMockImplClassType();
            ListResultHolder<CompareItem> resultHolder = new ListResultHolder<>();
            resultHolder.setCategoryName(categoryType.getDisplayName());
            decodedListResult.add(resultHolder);
            List<CompareItem> recordList = zstdDeserialize(stringListResultHolder.getRecord(),
                    mockImplClass);
            resultHolder.setRecord(recordList);
            List<CompareItem> replayResultList = zstdDeserialize(stringListResultHolder.getReplayResult(),
                    mockImplClass);
            resultHolder.setReplayResult(replayResultList);
        }
        return decodedListResult;
    }

    private List<CompareItem> zstdDeserialize(List<String> base64List, Class<?> clazz) {
        if (CollectionUtils.isEmpty(base64List)) {
            return Collections.emptyList();
        }
        List<CompareItem> decodedResult = new ArrayList<>(base64List.size());
        for (int i = 0; i < base64List.size(); i++) {
            String base64 = base64List.get(i);
            Object source = zstdJacksonSerializer.deserialize(base64, clazz);
            CompareItem item = prepareCompareItemBuilder.build(source);
            if (item != null) {
                decodedResult.add(item);
            }
        }
        return decodedResult;
    }
}

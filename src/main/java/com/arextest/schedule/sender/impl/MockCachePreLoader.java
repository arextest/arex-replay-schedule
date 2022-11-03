package com.arextest.schedule.sender.impl;

import com.arextest.storage.model.replay.QueryMockCacheRequestType;
import com.arextest.storage.model.replay.QueryMockCacheResponseType;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Component
final class MockCachePreLoader {
    @Value("${arex.storage.cacheLoad.url}")
    private String cachePreloadUrl;
    @Value("${arex.storage.cacheRemove.url}")
    private String cacheRemoveUrl;

    @Resource
    private HttpWepServiceApiClient httpWepServiceApiClient;

    void removeMockSource(String replayId) {
        QueryMockCacheRequestType mockCacheRequestType = new QueryMockCacheRequestType();
        mockCacheRequestType.setRecordId(replayId);
        httpWepServiceApiClient.jsonPost(cacheRemoveUrl, mockCacheRequestType, QueryMockCacheResponseType.class);
    }

    void fillMockSource(String replayId) {
        QueryMockCacheRequestType mockCacheRequestType = new QueryMockCacheRequestType();
        mockCacheRequestType.setRecordId(replayId);
        httpWepServiceApiClient.jsonPost(cachePreloadUrl, mockCacheRequestType, QueryMockCacheResponseType.class);
    }
}
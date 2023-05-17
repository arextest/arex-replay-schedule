package com.arextest.schedule.sender.impl;

import com.arextest.model.replay.QueryMockCacheRequestType;
import com.arextest.model.replay.QueryMockCacheResponseType;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.model.plan.BuildReplayPlanType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.arextest.schedule.common.CommonConstant.PINNED;


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

    void fillMockSource(String replayId, int replayPlanType) {
        QueryMockCacheRequestType mockCacheRequestType = new QueryMockCacheRequestType();
        mockCacheRequestType.setRecordId(replayId);
        if (replayPlanType == BuildReplayPlanType.BY_FIXED_CASE.getValue()) {
            mockCacheRequestType.setSourceProvider(PINNED);
        }
        if (replayPlanType == BuildReplayPlanType.BY_ROLLING_CASE.getValue()) {
            mockCacheRequestType.setSourceProvider(CommonConstant.ROLLING);
        }
        httpWepServiceApiClient.jsonPost(cachePreloadUrl, mockCacheRequestType, QueryMockCacheResponseType.class);
    }
}
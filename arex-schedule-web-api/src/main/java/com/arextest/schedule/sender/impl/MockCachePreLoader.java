package com.arextest.schedule.sender.impl;

import static com.arextest.schedule.common.CommonConstant.PINNED;
import com.arextest.model.replay.QueryMockCacheRequestType;
import com.arextest.model.replay.QueryMockCacheResponseType;
import com.arextest.model.response.ResponseStatusType;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.model.plan.BuildReplayPlanType;
import java.util.Optional;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class MockCachePreLoader {
  @Value("${arex.storage.cacheLoad.url}")
  private String cachePreloadUrl;
  @Value("${arex.storage.cacheRemove.url}")
  private String cacheRemoveUrl;
  @Resource
  private HttpWepServiceApiClient httpWepServiceApiClient;

  public QueryMockCacheResponseType removeMockSource(String replayId) {
    QueryMockCacheRequestType mockCacheRequestType = new QueryMockCacheRequestType();
    mockCacheRequestType.setRecordId(replayId);
    return httpWepServiceApiClient.jsonPost(cacheRemoveUrl, mockCacheRequestType,
        QueryMockCacheResponseType.class);
  }

  public QueryMockCacheResponseType fillMockSource(String replayId, int replayPlanType) {
    QueryMockCacheRequestType mockCacheRequestType = new QueryMockCacheRequestType();
    mockCacheRequestType.setRecordId(replayId);
    if (replayPlanType == BuildReplayPlanType.BY_PINNED_CASE.getValue()) {
      mockCacheRequestType.setSourceProvider(PINNED);
      return httpWepServiceApiClient.jsonPost(cachePreloadUrl, mockCacheRequestType,
          QueryMockCacheResponseType.class);
    } else if (replayPlanType == BuildReplayPlanType.MIXED.getValue()) {
      // todo: remove this code after the new version of arex-agent is released
      mockCacheRequestType.setSourceProvider(CommonConstant.AUTO_PINED);
      QueryMockCacheResponseType res = httpWepServiceApiClient.jsonPost(cachePreloadUrl,
          mockCacheRequestType, QueryMockCacheResponseType.class);
      if (!Optional.ofNullable(res)
          .map(QueryMockCacheResponseType::getResponseStatusType)
          .map(ResponseStatusType::getResponseCode)
          .map(code -> code.equals(0))
          .orElse(false)) {
        mockCacheRequestType.setSourceProvider(CommonConstant.ROLLING);
        return httpWepServiceApiClient.retryJsonPost(cachePreloadUrl, mockCacheRequestType,
            QueryMockCacheResponseType.class);
      } else {
        return res;
      }
    } else {
      return httpWepServiceApiClient.retryJsonPost(cachePreloadUrl, mockCacheRequestType,
          QueryMockCacheResponseType.class);
    }
  }
}
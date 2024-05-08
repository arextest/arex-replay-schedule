package com.arextest.schedule.sender.impl;

import com.arextest.model.replay.QueryMockCacheRequestType;
import com.arextest.model.replay.QueryMockCacheResponseType;
import com.arextest.model.response.ResponseStatusType;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.model.CaseProvider;
import com.arextest.schedule.model.ReplayActionCaseItem;
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

  public void removeCache(String replayId) {
    QueryMockCacheRequestType mockCacheRequestType = new QueryMockCacheRequestType();
    mockCacheRequestType.setRecordId(replayId);
    httpWepServiceApiClient.jsonPost(cacheRemoveUrl, mockCacheRequestType,
        QueryMockCacheResponseType.class);
  }

  public boolean prepareCache(ReplayActionCaseItem caseItem,
      int replayPlanType) {
    String recordId = caseItem.getRecordId();
    QueryMockCacheRequestType request = new QueryMockCacheRequestType();
    request.setRecordId(recordId);
    QueryMockCacheResponseType response;
    if (caseItem.getCaseProviderCode() == null) {
      if (replayPlanType == BuildReplayPlanType.BY_PINNED_CASE.getValue()) {
        request.setSourceProvider(CaseProvider.PINNED.getName());
      } else {
        request.setSourceProvider(CaseProvider.ROLLING.getName());
      }
    } else {
      String provider = Optional.ofNullable(CaseProvider.fromCode(caseItem.getCaseProviderCode()))
          .orElse(CaseProvider.ROLLING)
          .getName();
      request.setSourceProvider(provider);
    }

    response = httpWepServiceApiClient.retryJsonPost(cachePreloadUrl, request,
        QueryMockCacheResponseType.class);
    return isLoadingSuccess(response);
  }

  private static Boolean isLoadingSuccess(QueryMockCacheResponseType res) {
    return Optional.ofNullable(res)
        .map(QueryMockCacheResponseType::getResponseStatusType)
        .map(ResponseStatusType::getResponseCode)
        .map(code -> code.equals(0))
        .orElse(false);
  }
}
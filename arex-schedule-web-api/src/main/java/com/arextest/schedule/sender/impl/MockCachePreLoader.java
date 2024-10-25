package com.arextest.schedule.sender.impl;

import com.arextest.model.replay.QueryMockCacheRequestType;
import com.arextest.model.replay.QueryMockCacheResponseType;
import com.arextest.model.response.ResponseStatusType;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.model.CaseProviderEnum;
import com.arextest.schedule.model.ReplayActionCaseItem;
import jakarta.annotation.Resource;
import java.util.Optional;
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

  public boolean prepareCache(ReplayActionCaseItem caseItem) {
    String recordId = caseItem.getRecordId();
    QueryMockCacheRequestType request = new QueryMockCacheRequestType();
    request.setRecordId(recordId);
    QueryMockCacheResponseType response;

    String provider = Optional.ofNullable(CaseProviderEnum.fromCode(caseItem.getCaseProviderCode()))
        .orElse(CaseProviderEnum.ROLLING)
        .getName();
    request.setSourceProvider(provider);

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
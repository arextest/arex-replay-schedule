package com.arextest.schedule.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.arextest.model.replay.PagedResponseType;
import com.arextest.model.replay.QueryCaseCountResponseType;
import com.arextest.model.replay.ViewRecordResponseType;
import com.arextest.model.response.ResponseCode;
import com.arextest.model.response.ResponseStatusType;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

class ReplayCaseRemoteLoadServiceTest extends TestBase {

  @InjectMocks
  ReplayCaseRemoteLoadService service;

  @Mock
  private HttpWepServiceApiClient wepApiClientService;
  @Mock
  private ObjectMapper objectMapper;
  @Mock
  private MetricService metricService;

  @Test
  void queryCaseCount() {
    QueryCaseCountResponseType queryCount = new QueryCaseCountResponseType();
    ResponseStatusType resStatus = new ResponseStatusType();
    resStatus.setResponseCode(ResponseCode.SUCCESS.getCodeValue());
    queryCount.setResponseStatusType(resStatus);

    int res = service.queryCaseCount(prepareAction(), "TEST");
    assertEquals(0, res);

    // test empty
    res = service.queryCaseCount(prepareActionWithParent(), "TEST");
    assertEquals(0, res);

    // test valid response
    queryCount.setCount(1L);
    Mockito.when(wepApiClientService.jsonPost(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(queryCount);
    res = service.queryCaseCount(prepareActionWithParent(), "TEST");
    assertEquals(1, res);

  }

  @Test
  void viewReplayLoad() {
    ViewRecordResponseType queryResponse = new ViewRecordResponseType();
    ResponseStatusType resStatus = new ResponseStatusType();
    resStatus.setResponseCode(ResponseCode.SUCCESS.getCodeValue());
    queryResponse.setResponseStatusType(resStatus);

    // test null res
    ReplayActionCaseItem res = service.viewReplayLoad(prepareCaseWithParent(), new HashSet<>());

    // test empty res
    Mockito.when(wepApiClientService.jsonPost(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(queryResponse);
    res = service.viewReplayLoad(prepareCaseWithParent(), Collections.singleton("TEST"));

    // test valid response
    queryResponse.setRecordResult(Collections.singletonList(prepareMocker()));
    res = service.viewReplayLoad(prepareCaseWithParent(), Collections.singleton("TEST"));
    assertNotNull(res);
  }

  @Test
  void pagingLoad() {
    PagedResponseType queryResponse = new PagedResponseType();
    ResponseStatusType resStatus = new ResponseStatusType();
    resStatus.setResponseCode(ResponseCode.SUCCESS.getCodeValue());
    queryResponse.setResponseStatusType(resStatus);

    // test null res
    List<ReplayActionCaseItem> res = service
        .pagingLoad(1L, 1L, prepareActionWithParent(), 1, "TEST");

    // test empty res
    Mockito.when(wepApiClientService.jsonPost(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(queryResponse);
    res = service
        .pagingLoad(1L, 1L, prepareActionWithParent(), 1, "TEST");

    // test valid response
    queryResponse.setRecords(Collections.singletonList(prepareMocker()));
    res = service
        .pagingLoad(1L, 1L, prepareActionWithParent(), 1, "TEST");
    assertNotNull(res);
  }
}
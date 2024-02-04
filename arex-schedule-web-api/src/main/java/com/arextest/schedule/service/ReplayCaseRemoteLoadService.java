package com.arextest.schedule.service;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.model.replay.PagedRequestType;
import com.arextest.model.replay.PagedResponseType;
import com.arextest.model.replay.QueryCaseCountResponseType;
import com.arextest.model.replay.SortingOption;
import com.arextest.model.replay.SortingTypeEnum;
import com.arextest.model.replay.ViewRecordRequestType;
import com.arextest.model.replay.ViewRecordResponseType;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.model.CaseSendStatusType;
import com.arextest.schedule.model.CompareProcessStatusType;
import com.arextest.schedule.model.LogType;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.utils.MapUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;


/**
 * Created by wang_yc on 2021/9/15
 */
@Service
@Slf4j
public class ReplayCaseRemoteLoadService {

  private static final int EMPTY_SIZE = 0;
  private static final int AUTO_PINED_CASE_LIMIT = 20000;
  private static final String CREATE_TIME_COLUMN_NAME = "creationTime";
  @Resource
  private HttpWepServiceApiClient wepApiClientService;
  @Value("${arex.storage.viewRecord.url}")
  private String viewRecordUrl;
  @Value("${arex.storage.countByRange.url}")
  private String countByRangeUrl;
  @Value("${arex.storage.replayCase.url}")
  private String replayCaseUrl;
  @Resource
  private ObjectMapper objectMapper;
  @Resource
  private MetricService metricService;

  public int queryCaseCount(ReplayActionItem replayActionItem, String providerName) {
    int queryTotalCount = EMPTY_SIZE;
    try {
      int caseCountLimit =
          replayActionItem.getOperationTypes() == null ? replayActionItem.getParent()
              .getCaseCountLimit()
              : replayActionItem.getParent().getCaseCountLimit()
                  * replayActionItem.getOperationTypes().size();
      caseCountLimit =
          CommonConstant.AUTO_PINED.equals(providerName) ? AUTO_PINED_CASE_LIMIT : caseCountLimit;
      List<PagedRequestType> request = buildPagingSearchCaseRequests(replayActionItem,
          caseCountLimit, providerName);
      for (PagedRequestType pagedRequestType : request) {
        QueryCaseCountResponseType responseType =
            wepApiClientService.jsonPost(countByRangeUrl, pagedRequestType,
                QueryCaseCountResponseType.class);
        if (responseType == null || responseType.getResponseStatusType().hasError()) {
          continue;
        }
        queryTotalCount =
            queryTotalCount + Math.min(replayActionItem.getParent().getCaseCountLimit(),
                (int) responseType.getCount());
      }
    } catch (Exception e) {
      LOGGER.error("query case count error,request: {} ", replayActionItem.getId(), e);
    }
    return queryTotalCount;
  }

  public ReplayActionCaseItem viewReplayLoad(ReplayActionCaseItem caseItem, String sourceProvider,
      String operationType) {
    try {
      ViewRecordRequestType viewReplayCaseRequest = new ViewRecordRequestType();
      viewReplayCaseRequest.setRecordId(caseItem.getRecordId());
      viewReplayCaseRequest.setCategoryType(operationType);
      viewReplayCaseRequest.setSourceProvider(sourceProvider);

      ViewRecordResponseType responseType = wepApiClientService.jsonPost(viewRecordUrl,
          viewReplayCaseRequest,
          ViewRecordResponseType.class);
      if (responseType == null || responseType.getResponseStatusType().hasError()) {
        LOGGER.warn("view record response invalid recordId:{},response:{}",
            viewReplayCaseRequest.getRecordId(), responseType);
        return null;
      }
      List<AREXMocker> recordResultList = responseType.getRecordResult();
      if (CollectionUtils.isEmpty(recordResultList)) {
        LOGGER.warn("view record response empty result recordId:{}",
            viewReplayCaseRequest.getRecordId());
        return null;
      }
      return toCaseItem(recordResultList.get(0));

    } catch (Throwable e) {
      LOGGER.error("view record error: {},recordId: {}", e.getMessage(), caseItem.getRecordId(), e);
    }
    return null;
  }

  public ReplayActionCaseItem viewReplayLoad(ReplayActionCaseItem caseItem,
      Set<String> operationTypes) {
    ReplayActionCaseItem viewReplay = null;
    if (CollectionUtils.isEmpty(operationTypes)) {
      viewReplay = viewReplayLoad(caseItem, caseItem.getSourceProvider(),
          caseItem.getParent().getActionType());
    } else {
      for (String operationType : operationTypes) {
        viewReplay = viewReplayLoad(caseItem, caseItem.getSourceProvider(), operationType);
        if (viewReplay != null) {
          break;
        }
      }
    }
    return viewReplay;
  }

  private ReplayActionCaseItem toCaseItem(AREXMocker mainEntry) {
    Target targetRequest = mainEntry.getTargetRequest();
    if (targetRequest == null) {
      return null;
    }
    ReplayActionCaseItem caseItem = new ReplayActionCaseItem();
    caseItem.setTargetRequest(targetRequest);
    caseItem.setRecordId(mainEntry.getRecordId());
    caseItem.setRecordTime(mainEntry.getCreationTime());
    caseItem.setCaseType(mainEntry.getCategoryType().getName());
    caseItem.setSendStatus(CaseSendStatusType.WAIT_HANDLING.getValue());
    caseItem.setCompareStatus(CompareProcessStatusType.WAIT_HANDLING.getValue());
    caseItem.setSourceResultId(StringUtils.EMPTY);
    caseItem.setTargetResultId(StringUtils.EMPTY);

    return caseItem;
  }

  public List<ReplayActionCaseItem> pagingLoad(long beginTimeMills, long endTimeMills,
      ReplayActionItem replayActionItem, int caseCountLimit, String providerName) {
    List<AREXMocker> recordList = new ArrayList<>(caseCountLimit);
    List<PagedRequestType> requestTypeList = buildPagingSearchCaseRequests(replayActionItem,
        caseCountLimit, providerName);

    SortingOption sortingOption = new SortingOption();
    sortingOption.setLabel(CREATE_TIME_COLUMN_NAME);
    sortingOption.setSortingType(SortingTypeEnum.DESCENDING.getCode());
    List<SortingOption> sortingOptions = Collections.singletonList(sortingOption);
    for (PagedRequestType requestType : requestTypeList) {
      requestType.setBeginTime(beginTimeMills);
      requestType.setEndTime(endTimeMills);
      requestType.setSortingOptions(sortingOptions);
      PagedResponseType responseType;
      StopWatch watch = new StopWatch();
      watch.start(LogType.LOAD_CASE_TIME.getValue());
      responseType = wepApiClientService.jsonPost(replayCaseUrl, requestType,
          PagedResponseType.class);
      watch.stop();
      LOGGER.info("get replay case app id:{},time used:{} ms, operation:{}",
          requestType.getAppId(),
          watch.getTotalTimeMillis(), requestType.getOperation()
      );
      metricService.recordTimeEvent(LogType.LOAD_CASE_TIME.getValue(), replayActionItem.getPlanId(),
          replayActionItem.getAppId(), null, watch.getTotalTimeMillis());
      if (badResponse(responseType)) {
        try {
          LOGGER.warn("get replay case is empty,request:{} , response:{}",
              objectMapper.writeValueAsString(requestType),
              objectMapper.writeValueAsString(responseType));
        } catch (JsonProcessingException e) {
          LOGGER.error(e.getMessage(), e);
        }
        continue;
      }
      recordList.addAll(responseType.getRecords());
    }
    return toCaseItemList(recordList);
  }

  private boolean badResponse(PagedResponseType responseType) {
    if (responseType == null) {
      return true;
    }
    if (responseType.getResponseStatusType().hasError()) {
      return true;
    }
    return CollectionUtils.isEmpty(responseType.getRecords());
  }

  private List<ReplayActionCaseItem> toCaseItemList(List<AREXMocker> source) {
    List<ReplayActionCaseItem> caseItemList = new ArrayList<>(source.size());
    for (AREXMocker soa : source) {
      ReplayActionCaseItem caseItem = toCaseItem(soa);
      caseItemList.add(caseItem);
    }
    return caseItemList;
  }

  private PagedRequestType buildPagingSearchCaseRequest(ReplayActionItem replayActionItem,
      int caseCountLimit, String providerName) {
    ReplayPlan parent = replayActionItem.getParent();
    PagedRequestType requestType = new PagedRequestType();
    requestType.setAppId(parent.getAppId());
    requestType.setPageSize(Math.min(CommonConstant.MAX_PAGE_SIZE, caseCountLimit));
    requestType.setEnv(parent.getCaseSourceType());
    requestType.setBeginTime(parent.getCaseSourceFrom().getTime());
    requestType.setEndTime(parent.getCaseSourceTo().getTime());
    requestType.setOperation(replayActionItem.getOperationName());
    requestType.setCategory(MockCategoryType.createEntryPoint(replayActionItem.getActionType()));
    requestType.setSourceProvider(providerName);
    // add the condition of "caseTag"
    if (MapUtils.isNotEmpty(parent.getCaseTags())){
      requestType.setTags(parent.getCaseTags());
    }
    return requestType;
  }

  private List<PagedRequestType> buildPagingSearchCaseRequests(ReplayActionItem replayActionItem,
      int caseCountLimit, String providerName) {
    if (CollectionUtils.isEmpty(replayActionItem.getOperationTypes())) {
      return Arrays.asList(
          buildPagingSearchCaseRequest(replayActionItem, caseCountLimit, providerName));
    }
    List<PagedRequestType> pagedRequestTypeList = new ArrayList<>();
    for (String catagoryType : replayActionItem.getOperationTypes()) {
      PagedRequestType pagedRequestType = buildPagingSearchCaseRequest(replayActionItem,
          (int) Math.ceil(caseCountLimit / replayActionItem.getOperationTypes().size()),
          providerName);
      pagedRequestType.setCategory(MockCategoryType.createEntryPoint(catagoryType));
      pagedRequestTypeList.add(pagedRequestType);
    }
    return pagedRequestTypeList;
  }

}
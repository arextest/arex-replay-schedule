package com.arextest.schedule.service;


import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.model.replay.PagedRequestType;
import com.arextest.model.replay.PagedResponseType;
import com.arextest.model.replay.QueryCaseCountResponseType;
import com.arextest.model.replay.ViewRecordRequestType;
import com.arextest.model.replay.ViewRecordResponseType;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.model.CaseSendStatusType;
import com.arextest.schedule.model.CompareProcessStatusType;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Created by wang_yc on 2021/9/15
 */
@Service
@Slf4j
public class ReplayCaseRemoteLoadService {
    @Resource
    private HttpWepServiceApiClient wepApiClientService;
    private static final int EMPTY_SIZE = 0;
    @Value("${arex.storage.viewRecord.url}")
    private String viewRecordUrl;
    @Value("${arex.storage.countByRange.url}")
    private String countByRangeUrl;
    @Value("${arex.storage.replayCase.url}")
    private String replayCaseUrl;
    @Resource
    private ObjectMapper objectMapper;


    public int queryCaseCount(ReplayActionItem replayActionItem) {
        try {
            int caseCountLimit = replayActionItem.getParent().getCaseCountLimit();
            PagedRequestType request = buildPagingSearchCaseRequest(replayActionItem, caseCountLimit);
            QueryCaseCountResponseType responseType =
                    wepApiClientService.jsonPost(countByRangeUrl, request, QueryCaseCountResponseType.class);
            if (responseType == null || responseType.getResponseStatusType().hasError()) {
                return EMPTY_SIZE;
            }
            return Math.min(caseCountLimit, (int) responseType.getCount());
        } catch (Exception e) {
            LOGGER.error("query case count error,request: {} ", replayActionItem.getId(), e);
        }
        return EMPTY_SIZE;
    }

    public ReplayActionCaseItem viewReplayLoad(ReplayActionCaseItem caseItem) {
        try {
            ViewRecordRequestType viewReplayCaseRequest = new ViewRecordRequestType();
            viewReplayCaseRequest.setRecordId(caseItem.getRecordId());
            viewReplayCaseRequest.setCategoryType(caseItem.getCaseType());
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
                                                 ReplayActionItem replayActionItem, int caseCountLimit) {
        PagedRequestType requestType = buildPagingSearchCaseRequest(replayActionItem, caseCountLimit);
        requestType.setBeginTime(beginTimeMills);
        requestType.setEndTime(endTimeMills);
        PagedResponseType responseType;
        long beginTime = System.currentTimeMillis();
        responseType = wepApiClientService.jsonPost(replayCaseUrl, requestType, PagedResponseType.class);
        LOGGER.info("get replay case app id:{},time used:{} ms, operation:{}",
                requestType.getAppId(),
                System.currentTimeMillis() - beginTime, requestType.getOperation()
        );
        if (badResponse(responseType)) {
            try {
                LOGGER.warn("get replay case is empty,request:{} , response:{}",
                        objectMapper.writeValueAsString(requestType), objectMapper.writeValueAsString(responseType));
            } catch (JsonProcessingException e) {
                LOGGER.error(e.getMessage(), e);
            }
            return Collections.emptyList();
        }

        return toCaseItemList(responseType.getRecords());
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

    private PagedRequestType buildPagingSearchCaseRequest(ReplayActionItem replayActionItem, int caseCountLimit) {
        ReplayPlan parent = replayActionItem.getParent();
        PagedRequestType requestType = new PagedRequestType();
        requestType.setAppId(parent.getAppId());
        requestType.setPageSize(Math.min(CommonConstant.MAX_PAGE_SIZE, caseCountLimit));
        requestType.setEnv(parent.getCaseSourceType());
        requestType.setBeginTime(parent.getCaseSourceFrom().getTime());
        requestType.setEndTime(parent.getCaseSourceTo().getTime());
        requestType.setOperation(replayActionItem.getOperationName());
        requestType.setCategory(MockCategoryType.createEntryPoint(replayActionItem.getActionType()));
        return requestType;
    }

}
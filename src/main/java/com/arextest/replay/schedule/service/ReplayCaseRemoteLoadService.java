package com.arextest.replay.schedule.service;

import com.arextest.replay.schedule.serialization.ZstdJacksonSerializer;
import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.MainEntry;
import com.arextest.storage.model.replay.PagingQueryCaseRequestType;
import com.arextest.storage.model.replay.PagingQueryCaseResponseType;
import com.arextest.storage.model.replay.QueryCaseCountResponseType;
import com.arextest.storage.model.replay.ReplayCaseRangeRequestType;
import com.arextest.storage.model.replay.ViewRecordRequestType;
import com.arextest.storage.model.replay.ViewRecordResponseType;
import com.arextest.replay.schedule.client.HttpWepServiceApiClient;
import com.arextest.replay.schedule.common.CommonConstant;
import com.arextest.replay.schedule.model.CaseSendStatusType;
import com.arextest.replay.schedule.model.CompareProcessStatusType;
import com.arextest.replay.schedule.model.ReplayActionCaseItem;
import com.arextest.replay.schedule.model.ReplayActionItem;
import com.arextest.replay.schedule.model.ReplayPlan;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    @Resource
    private ZstdJacksonSerializer zstdJacksonSerializer;

    public int queryCaseCount(ReplayActionItem replayActionItem) {
        try {
            ReplayCaseRangeRequestType request = buildPagingSearchCaseRequest(replayActionItem);
            QueryCaseCountResponseType responseType =
                    wepApiClientService.jsonPost(countByRangeUrl, request, QueryCaseCountResponseType.class);
            if (responseType == null || responseType.getResponseStatusType().hasError()) {
                return EMPTY_SIZE;
            }
            return Math.min(CommonConstant.OPERATION_MAX_CASE_COUNT, responseType.getCount());
        } catch (Exception e) {
            LOGGER.error("query case count error,request: {} ", replayActionItem.getId(), e);
        }
        return EMPTY_SIZE;
    }

    public ReplayActionCaseItem viewReplayLoad(ReplayActionCaseItem caseItem) {
        try {
            ViewRecordRequestType viewReplayCaseRequest = new ViewRecordRequestType();
            viewReplayCaseRequest.setRecordId(caseItem.getRecordId());
            ViewRecordResponseType responseType = wepApiClientService.jsonPost(viewRecordUrl,
                    viewReplayCaseRequest,
                    ViewRecordResponseType.class);
            if (responseType == null || responseType.getResponseStatusType().hasError()) {
                LOGGER.warn("view record response invalid recordId:{},response:{}",
                        viewReplayCaseRequest.getRecordId(), responseType);
                return null;
            }
            Map<Integer, List<String>> recordResultMap = responseType.getRecordResult();
            if (MapUtils.isEmpty(recordResultMap)) {
                LOGGER.warn("view record response empty result recordId:{}",
                        viewReplayCaseRequest.getRecordId());
                return null;
            }
            for (Map.Entry<Integer, List<String>> categoryListEntry : recordResultMap.entrySet()) {
                MockCategoryType mockCategoryType = MockCategoryType.of(categoryListEntry.getKey());
                if (mockCategoryType.isMainEntry()) {
                    List<String> mainResult = categoryListEntry.getValue();
                    if (CollectionUtils.isEmpty(mainResult)) {
                        LOGGER.warn("view record response not found main entry result recordId:{}",
                                viewReplayCaseRequest.getRecordId());
                        return null;
                    }
                    MainEntry mainEntry = (MainEntry) zstdJacksonSerializer.deserialize(mainResult.get(0),
                            mockCategoryType.getMockImplClassType());
                    if (mainEntry != null) {
                        return toCaseItem(mainEntry);
                    }
                }
            }

        } catch (Throwable e) {
            LOGGER.error("view record error: {},recordId: {}", e.getMessage(), caseItem.getRecordId(), e);
        }
        return null;
    }


    private ReplayActionCaseItem toCaseItem(MainEntry mainEntry) {
        ReplayActionCaseItem caseItem = new ReplayActionCaseItem();
        caseItem.setRecordId(mainEntry.getRecordId());
        if (mainEntry.getConfigVersion() != null) {
            // TODO:change defined type
            caseItem.setReplayDependency(String.valueOf(mainEntry.getConfigVersion()));
        }
        String message = mainEntry.getRequest();
        if (message == null) {
            message = StringUtils.EMPTY;
        }
        caseItem.setRequestMessage(message);
        caseItem.setRequestMessageFormat(mainEntry.getFormat());
        caseItem.setConsumeGroup(mainEntry.getConsumerGroupName());
        caseItem.setRecordTime(mainEntry.getCreateTime());
        caseItem.setCaseType(mainEntry.getCategoryType());
        caseItem.setSendStatus(CaseSendStatusType.WAIT_HANDLING.getValue());
        caseItem.setCompareStatus(CompareProcessStatusType.WAIT_HANDLING.getValue());
        caseItem.setSourceResultId(StringUtils.EMPTY);
        caseItem.setTargetResultId(StringUtils.EMPTY);
        caseItem.setRequestMethod(mainEntry.getMethod());
        caseItem.setRequestHeaders(mainEntry.getRequestHeaders());
        caseItem.setRequestPath(mainEntry.getPath());
        return caseItem;
    }

    public List<ReplayActionCaseItem> pagingLoad(long beginTimeMills, long endTimeMills,
                                                 ReplayActionItem replayActionItem) {
        PagingQueryCaseRequestType requestType = buildPagingSearchCaseRequest(replayActionItem);
        requestType.setBeginTime(beginTimeMills);
        requestType.setEndTime(endTimeMills);
        PagingQueryCaseResponseType responseType;
        long beginTime = System.currentTimeMillis();
        responseType = wepApiClientService.jsonPost(replayCaseUrl, requestType, PagingQueryCaseResponseType.class);
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

        return toCaseItemList(responseType.getMainEntryList());
    }

    private boolean badResponse(PagingQueryCaseResponseType responseType) {
        if (responseType == null) {
            return true;
        }
        if (responseType.getResponseStatusType().hasError()) {
            return true;
        }
        return CollectionUtils.isEmpty(responseType.getMainEntryList());
    }

    private List<ReplayActionCaseItem> toCaseItemList(List<? extends MainEntry> source) {
        List<ReplayActionCaseItem> caseItemList = new ArrayList<>(source.size());
        for (MainEntry soa : source) {
            ReplayActionCaseItem caseItem = toCaseItem(soa);
            caseItemList.add(caseItem);
        }
        return caseItemList;
    }

    private PagingQueryCaseRequestType buildPagingSearchCaseRequest(ReplayActionItem replayActionItem) {
        ReplayPlan parent = replayActionItem.getParent();
        PagingQueryCaseRequestType requestType = new PagingQueryCaseRequestType();
        requestType.setAppId(parent.getAppId());
        requestType.setMaxCaseCount(CommonConstant.MAX_PAGE_SIZE);
        requestType.setEnv(parent.getCaseSourceType());
        requestType.setBeginTime(parent.getCaseSourceFrom().getTime());
        requestType.setEndTime(parent.getCaseSourceTo().getTime());
        requestType.setService(replayActionItem.getServiceKey());
        requestType.setOperation(replayActionItem.getOperationName());
        requestType.setCategoryType(replayActionItem.getActionType());
        return requestType;
    }

}

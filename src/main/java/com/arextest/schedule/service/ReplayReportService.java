package com.arextest.schedule.service;


import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.comparer.ComparisonWriter;
import com.arextest.common.model.response.GenericResponseType;
import com.arextest.common.model.response.Response;
import com.arextest.diff.sdk.CompareSDK;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayCompareResult;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.report.model.api.contracts.ChangeReplayStatusRequestType;
import com.arextest.report.model.api.contracts.PushCompareResultsRequestType;
import com.arextest.report.model.api.contracts.ReportInitialRequestType;
import com.arextest.report.model.api.contracts.common.CompareResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by wang_yc on 2021/10/19
 */
@Slf4j
@Component
public final class ReplayReportService implements ComparisonWriter {
    @Resource
    private HttpWepServiceApiClient httpWepServiceApiClient;
    @Value("${arex.report.init.url}")
    private String reportInitUrl;
    @Value("${arex.report.push.compareResult.url}")
    private String pushReplayCompareResultUrl;
    @Value("${arex.report.push.replayStatus.url}")
    private String pushReplayStatusUrl;

    public void initReportInfo(ReplayPlan replayPlan) {
        ReportInitialRequestType requestType = new ReportInitialRequestType();
        requestType.setPlanId(replayPlan.getId());
        requestType.setPlanName(replayPlan.getPlanName());
        requestType.setCreator(replayPlan.getOperator());
        requestType.setTotalCaseCount(replayPlan.getCaseTotalCount());
        // for case env
        ReportInitialRequestType.CaseSourceEnvironment caseSourceEnv;
        caseSourceEnv = new ReportInitialRequestType.CaseSourceEnvironment();
        caseSourceEnv.setCaseStartTime(replayPlan.getCaseSourceFrom().getTime());
        caseSourceEnv.setCaseEndTime(replayPlan.getCaseSourceTo().getTime());
        caseSourceEnv.setCaseSourceType(replayPlan.getCaseSourceType());
        requestType.setCaseSourceEnv(caseSourceEnv);
        // for app
        ReportInitialRequestType.Application application = new ReportInitialRequestType.Application();
        application.setAppId(replayPlan.getAppId());
        application.setAppName(replayPlan.getAppName());
        requestType.setApplication(application);
        // for host env
        ReportInitialRequestType.HostEnvironment hostEnv = new ReportInitialRequestType.HostEnvironment();
        hostEnv.setSourceEnv(replayPlan.getSourceEnv());
        hostEnv.setSourceHost(replayPlan.getSourceHost());
        hostEnv.setTargetEnv(replayPlan.getTargetEnv());
        hostEnv.setTargetHost(replayPlan.getTargetHost());
        requestType.setHostEnv(hostEnv);
        // for version
        ReportInitialRequestType.Version version = new ReportInitialRequestType.Version();
        version.setCaseRecordVersion(replayPlan.getCaseRecordVersion());
        version.setCoreVersion(replayPlan.getArexCordVersion());
        version.setExtVersion(replayPlan.getArexExtVersion());
        requestType.setVersion(version);
        // for image
        ReportInitialRequestType.TargetImage targetImage = new ReportInitialRequestType.TargetImage();
        targetImage.setTargetImageId(replayPlan.getTargetImageId());
        targetImage.setTargetImageName(replayPlan.getTargetImageName());
        requestType.setTargetImage(targetImage);
        // for plan actions
        List<ReplayActionItem> actionItemList = replayPlan.getReplayActionItemList();
        List<ReportInitialRequestType.ReportItem> reportItemList = new ArrayList<>(actionItemList.size());
        ReportInitialRequestType.ReportItem reportItem;
        for (ReplayActionItem actionItem : actionItemList) {
            reportItem = new ReportInitialRequestType.ReportItem();
            reportItem.setOperationId(actionItem.getOperationId());
            reportItem.setOperationName(actionItem.getOperationName());
            reportItem.setServiceName(actionItem.getServiceKey());
            reportItem.setPlanItemId(actionItem.getId());
            reportItem.setTotalCaseCount(Math.min(CommonConstant.OPERATION_MAX_CASE_COUNT, actionItem.getReplayCaseCount()));
            reportItemList.add(reportItem);
        }
        requestType.setReportItemList(reportItemList);
        LOGGER.info("initReport request:{}", requestType);
        Response response = httpWepServiceApiClient.jsonPost(reportInitUrl, requestType,
                GenericResponseType.class);
        LOGGER.info("initReport response:{}", response);
    }

    public void pushActionStatus(String planId, ReplayStatusType statusType, String actionId) {
        ChangeReplayStatusRequestType requestType = new ChangeReplayStatusRequestType();
        ChangeReplayStatusRequestType.ReplayItem replayItem = new ChangeReplayStatusRequestType.ReplayItem();
        replayItem.setPlanItemId(actionId);
        replayItem.setStatus(statusType.getValue());
        requestType.setPlanId(planId);
        requestType.setItems(Collections.singletonList(replayItem));
        Object response = httpWepServiceApiClient.jsonPost(pushReplayStatusUrl, requestType,
                GenericResponseType.class);
        LOGGER.info("push action status actionId: {},status: {}, result:{}", actionId,
                statusType, response);
    }

    public void pushPlanStatus(String planId, ReplayStatusType statusType) {
        ChangeReplayStatusRequestType requestType = new ChangeReplayStatusRequestType();
        requestType.setPlanId(planId);
        requestType.setStatus(statusType.getValue());
        Object response = httpWepServiceApiClient.jsonPost(pushReplayStatusUrl, requestType,
                GenericResponseType.class);
        LOGGER.info("push plan status planId: {},status: {}, result:{}", planId, statusType, response);
    }

    @Override
    public boolean write(List<ReplayCompareResult> comparedResult) {
        if (CollectionUtils.isEmpty(comparedResult)) {
            return true;
        }
        int comparedSize = comparedResult.size();
        PushCompareResultsRequestType requestType = new PushCompareResultsRequestType();
        List<CompareResult> results = new ArrayList<>(comparedSize);
        CompareResult requestResult;
        ReplayCompareResult sourceResult;
        ReportResultConverter converter = ReportResultConverter.DEFAULT;
        for (int i = 0; i < comparedSize; i++) {
            sourceResult = comparedResult.get(i);
            requestResult = converter.to(sourceResult);
            results.add(requestResult);
        }
        requestType.setResults(results);
        Response response = httpWepServiceApiClient.jsonPost(pushReplayCompareResultUrl, requestType,
                GenericResponseType.class);
        if (response == null || response.getResponseStatusType().hasError()) {
            LOGGER.warn("push replay compared result to report size: {}, result:{}", comparedSize, response);
        }
        return true;
    }

    @Override
    public boolean writeIncomparable(ReplayActionCaseItem caseItem, String remark) {
        com.arextest.diff.model.CompareResult sdkResult =
                CompareSDK.fromException(caseItem.requestMessage(), null, remark);
        ReplayCompareResult replayCompareResult = ReplayCompareResult.createFrom(caseItem, sdkResult);
        replayCompareResult.setDiffResultCode(sdkResult.getCode());
        return this.write(Collections.singletonList(replayCompareResult));
    }
}
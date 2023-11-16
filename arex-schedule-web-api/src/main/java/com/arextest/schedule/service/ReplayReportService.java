package com.arextest.schedule.service;

import com.arextest.common.model.response.GenericResponseType;
import com.arextest.common.model.response.Response;
import com.arextest.diff.model.enumeration.DiffResultCode;
import com.arextest.diff.sdk.CompareSDK;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.comparer.ComparisonWriter;
import com.arextest.schedule.dao.mongodb.ReplayCompareResultRepositoryImpl;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayCompareResult;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.model.converter.ReplayCompareResultConverter;
import com.arextest.web.model.contract.contracts.ChangeReplayStatusRequestType;
import com.arextest.web.model.contract.contracts.RemoveErrorMsgRequest;
import com.arextest.web.model.contract.contracts.RemoveRecordsAndScenesRequest;
import com.arextest.web.model.contract.contracts.ReportInitialRequestType;
import com.arextest.web.model.contract.contracts.replay.AnalyzeCompareResultsRequestType;
import com.arextest.web.model.contract.contracts.replay.UpdateReportInfoRequestType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by wang_yc on 2021/10/19
 */
@Slf4j
@Component
public final class ReplayReportService implements ComparisonWriter {

  private static final String CASE_COUNT_LIMIT_NAME = "caseCountLimit";
  @Resource
  private ReplayCompareResultRepositoryImpl replayCompareResultRepository;
  @Resource
  private ReplayCompareResultConverter converter;
  @Resource
  private HttpWepServiceApiClient httpWepServiceApiClient;
  @Value("${arex.api.init.url}")
  private String reportInitUrl;
  @Value("${arex.api.push.compareResult.url}")
  private String pushReplayCompareResultUrl;
  @Value("${arex.api.push.replayStatus.url}")
  private String pushReplayStatusUrl;
  @Value("${arex.api.update.report.info.url}")
  private String updateReportInfoUrl;
  @Value("${arex.api.remove.records.url}")
  private String removeRecordsUrl;
  @Value("${arex.api.remove.errorMsg.url}")
  private String removeErrorMsgUrl;

  public boolean initReportInfo(ReplayPlan replayPlan) {
    ReportInitialRequestType requestType = new ReportInitialRequestType();
    requestType.setPlanId(replayPlan.getId());
    requestType.setPlanName(replayPlan.getPlanName());
    requestType.setCreator(replayPlan.getOperator());
    requestType.setTotalCaseCount(replayPlan.getCaseTotalCount());
    Map<String, Object> customTags = new HashMap<>();
    customTags.put(CASE_COUNT_LIMIT_NAME, replayPlan.getCaseCountLimit());
    requestType.setCustomTags(customTags);
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
    List<ReportInitialRequestType.ReportItem> reportItemList = new ArrayList<>(
        actionItemList.size());
    ReportInitialRequestType.ReportItem reportItem;
    for (ReplayActionItem actionItem : actionItemList) {
      reportItem = new ReportInitialRequestType.ReportItem();
      reportItem.setOperationId(actionItem.getOperationId());
      reportItem.setOperationName(actionItem.getOperationName());
      reportItem.setServiceName(actionItem.getServiceKey());
      reportItem.setPlanItemId(actionItem.getId());
      reportItem.setTotalCaseCount(actionItem.getReplayCaseCount());
      reportItemList.add(reportItem);
    }
    requestType.setReportItemList(reportItemList);
    LOGGER.info("initReport request:{}", requestType);
    Response response = httpWepServiceApiClient.jsonPost(reportInitUrl, requestType,
        GenericResponseType.class);
    LOGGER.info("initReport request:{}, response:{}", requestType, response);
    return response != null && !response.getResponseStatusType().hasError();
  }

  public void updateReportCaseCount(ReplayPlan replayPlan) {
    UpdateReportInfoRequestType requestType = new UpdateReportInfoRequestType();
    requestType.setPlanId(replayPlan.getId());
    requestType.setTotalCaseCount(replayPlan.getCaseTotalCount());
    List<ReplayActionItem> actionItemList = replayPlan.getReplayActionItemList();
    if (CollectionUtils.isNotEmpty(actionItemList)) {
      List<UpdateReportInfoRequestType.UpdateReportItem> updateReportInfoList = new ArrayList<>(
          actionItemList.size());
      UpdateReportInfoRequestType.UpdateReportItem reportItem;
      for (ReplayActionItem actionItem : actionItemList) {
        reportItem = new UpdateReportInfoRequestType.UpdateReportItem();
        reportItem.setPlanItemId(actionItem.getId());
        reportItem.setTotalCaseCount(actionItem.getReplayCaseCount());
        updateReportInfoList.add(reportItem);
      }
      requestType.setUpdateReportItems(updateReportInfoList);
    }
    Response response = httpWepServiceApiClient.jsonPost(updateReportInfoUrl, requestType,
        GenericResponseType.class);
    LOGGER.info("updateReportCaseCount request:{}, response:{}", requestType, response);
  }

  public void pushActionStatus(String planId, ReplayStatusType statusType, String actionId,
      String errorMessage,
      boolean rerun) {
    ChangeReplayStatusRequestType requestType = new ChangeReplayStatusRequestType();
    ChangeReplayStatusRequestType.ReplayItem replayItem = new ChangeReplayStatusRequestType.ReplayItem();
    replayItem.setPlanItemId(actionId);
    replayItem.setStatus(statusType.getValue());
    replayItem.setErrorMessage(errorMessage);
    requestType.setPlanId(planId);
    requestType.setItems(Collections.singletonList(replayItem));
    requestType.setRerun(rerun);
    Object response = httpWepServiceApiClient.jsonPost(pushReplayStatusUrl, requestType,
        GenericResponseType.class);
    LOGGER.info("push action status actionId: {},status: {}, result:{}", actionId,
        statusType, response);
  }

  public void pushPlanStatus(String planId, ReplayStatusType statusType, String errorMessage,
      boolean rerun) {
    ChangeReplayStatusRequestType requestType = new ChangeReplayStatusRequestType();
    requestType.setPlanId(planId);
    requestType.setStatus(statusType.getValue());
    requestType.setErrorMessage(errorMessage);
    requestType.setRerun(rerun);
    Object response = httpWepServiceApiClient.jsonPost(pushReplayStatusUrl, requestType,
        GenericResponseType.class);
    LOGGER.info("push plan status planId: {},status: {}, result:{}", planId, statusType, response);
  }

  @Override
  public boolean write(List<ReplayCompareResult> comparedResult) {
    if (CollectionUtils.isEmpty(comparedResult)) {
      LOGGER.info("not write comparedResult");
      return true;
    }
    int comparedSize = comparedResult.size();

    AnalyzeCompareResultsRequestType request = new AnalyzeCompareResultsRequestType();
    List<AnalyzeCompareResultsRequestType.AnalyzeCompareInfoItem> reqItems = new ArrayList<>(
        comparedSize);

    ReplayCompareResult firstResult = comparedResult.get(0);
    this.replayCompareResultRepository.deleteByRecord(firstResult.getRecordId(),
        firstResult.getPlanItemId());
    this.replayCompareResultRepository.save(comparedResult);

    for (ReplayCompareResult sourceResult : comparedResult) {
      reqItems.add(converter.reportContractFromBo(sourceResult));
    }
    request.setAnalyzeCompareInfos(reqItems);

    Response response = httpWepServiceApiClient.jsonPost(pushReplayCompareResultUrl, request,
        GenericResponseType.class);
    if (response == null || response.getResponseStatusType().hasError()) {
      LOGGER.error("push replay compared result to report size: {}, result:{}", comparedSize,
          response);
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

  public boolean writeQmqCompareResult(ReplayActionCaseItem caseItem) {
    if (caseItem == null) {
      return true;
    }
    AnalyzeCompareResultsRequestType request = new AnalyzeCompareResultsRequestType();
    ReplayCompareResult compareResult = toQMQCompareResult(caseItem);

    this.replayCompareResultRepository.deleteByRecord(compareResult.getRecordId(),
        compareResult.getPlanItemId());
    this.replayCompareResultRepository.save(compareResult);

    request.setAnalyzeCompareInfos(
        Collections.singletonList(converter.reportContractFromBo(compareResult)));

    Response response = httpWepServiceApiClient.jsonPost(pushReplayCompareResultUrl, request,
        GenericResponseType.class);
    if (response == null || response.getResponseStatusType().hasError()) {
      LOGGER.error("writeQmqCompareResult to report result:{}", response);
    }
    return true;
  }

  private ReplayCompareResult toQMQCompareResult(ReplayActionCaseItem caseItem) {
    ReplayCompareResult compareResult = new ReplayCompareResult();
    ReplayActionItem parent = caseItem.getParent();
    compareResult.setPlanId(parent.getPlanId());
    compareResult.setOperationId(parent.getOperationId());
    compareResult.setServiceName(parent.getServiceName());
    compareResult.setCategoryName(MockCategoryType.Q_MESSAGE_CONSUMER.getName());
    compareResult.setDiffResultCode(DiffResultCode.COMPARED_WITHOUT_DIFFERENCE);
    compareResult.setOperationName(parent.getOperationName());
    compareResult.setReplayId(caseItem.getTargetResultId());
    compareResult.setRecordId(caseItem.getRecordId());
    compareResult.setPlanItemId(caseItem.getPlanItemId());
    compareResult.setCaseId(caseItem.getId());
    return compareResult;
  }

  public void removeRecordsAndScenes(Map<String, List<String>> actionIdAndRecordIdsMap) {
    RemoveRecordsAndScenesRequest requestType = new RemoveRecordsAndScenesRequest();
    requestType.setActionIdAndRecordIdsMap(actionIdAndRecordIdsMap);
    Response response = httpWepServiceApiClient.jsonPost(removeRecordsUrl, requestType,
        GenericResponseType.class);
    LOGGER.info("removeRecordsAndScenes request:{}, response:{}", requestType, response);
  }

  public void removeErrorMsg(String planId, List<String> planItemIdList) {
    RemoveErrorMsgRequest request = new RemoveErrorMsgRequest();
    request.setPlanId(planId);
    request.setPlanItemIdList(planItemIdList);
    Response response = httpWepServiceApiClient.jsonPost(removeErrorMsgUrl, request,
        GenericResponseType.class);
    LOGGER.info("removeErrorMsg request:{}, response:{}", request, response);
  }
}
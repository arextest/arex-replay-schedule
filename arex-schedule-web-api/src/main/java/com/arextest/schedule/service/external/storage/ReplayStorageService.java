package com.arextest.schedule.service.external.storage;

import com.arextest.model.replay.QueryReplayResultRequestType;
import com.arextest.model.replay.QueryReplayResultResponseType;
import com.arextest.model.replay.holder.ListResultHolder;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.model.CompareProcessStatusType;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.plan.BuildReplayPlanType;
import com.arextest.schedule.model.storage.PostProcessResultRequestType;
import com.arextest.schedule.model.storage.ResultCodeGroup;
import com.arextest.schedule.model.storage.ResultCodeGroup.CategoryGroup;
import com.arextest.schedule.model.storage.ResultCodeGroup.IdPair;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReplayStorageService {

  @Resource
  private HttpWepServiceApiClient httpClient;

  @Value("${arex.storage.postProcess.url}")
  private String postProcessUrl;
  @Value("${arex.storage.replayResult.url}")
  private String replayResultUrl;
  private static final int NORMAL_FINISH_CODE = 2;

  public void postProcessCompareResult(ReplayActionCaseItem caseItem) {
    try {
      // only handle normally compared case
      if (caseItem.getCompareStatus() != CompareProcessStatusType.HAS_DIFF.getValue() &&
          caseItem.getCompareStatus() != CompareProcessStatusType.PASS.getValue()) {
        return;
      }
      boolean mixedReplay = Optional.ofNullable(caseItem.getParent())
          .map(ReplayActionItem::getParent)
          .map(ReplayPlan::getReplayPlanType)
          .map(type -> type.equals(BuildReplayPlanType.MIXED.getValue()))
          .orElse(false);

      // only handle case with target result
      if (StringUtils.isEmpty(caseItem.getTargetResultId()) || !mixedReplay) {
        return;
      }

      PostProcessResultRequestType storageReq = new PostProcessResultRequestType();

      // originally only handle normal plan, refactored to case level handling
      storageReq.setReplayStatusCode(NORMAL_FINISH_CODE);
      storageReq.setReplayPlanId(caseItem.getPlanId());

      ResultCodeGroup resGroup = new ResultCodeGroup();
      storageReq.setResults(Collections.singletonList(resGroup));

      resGroup.setResultCode(caseItem.getCompareStatus());
      CategoryGroup category = new CategoryGroup();
      category.setCategoryName(caseItem.getCaseType());
      IdPair pair = new IdPair();
      pair.setRecordId(caseItem.getRecordId());
      pair.setTargetId(caseItem.getTargetResultId());

      category.setResultIds(Collections.singletonList(pair));
      resGroup.setCategoryGroups(Collections.singletonList(category));

      String out = httpClient.jsonPost(postProcessUrl, storageReq, String.class);
      LOGGER.info("postProcessCompareResult result: {}", out);
    } catch (Exception e) {
      LOGGER.error("postProcessCompareResult error", e);
    }
  }

  /**
   * query replay result
   * @param recordId record id
   * @param replayId replay id
   * @return List of ListResultHolder
   */
  public List<ListResultHolder> queryRelayResult(String recordId, String replayId) {
    QueryReplayResultRequestType request = new QueryReplayResultRequestType();
    request.setRecordId(recordId);
    request.setReplayResultId(replayId);
    QueryReplayResultResponseType response = httpClient.retryJsonPost(replayResultUrl, request,
        QueryReplayResultResponseType.class);
    if (!checkReplayResultResponse(response)) {
      return Collections.emptyList();
    }
    return response.getResultHolderList();
  }

  private boolean checkReplayResultResponse(QueryReplayResultResponseType response) {
    if (response == null || response.getResponseStatusType() == null) {
      return false;
    }
    if (response.getResponseStatusType().hasError()) {
      LOGGER.warn("query replay result has error response : {}", response.getResponseStatusType());
      return false;
    }
    List<ListResultHolder> resultHolderList = response.getResultHolderList();
    if (CollectionUtils.isEmpty(resultHolderList)) {
      LOGGER.warn("query replay result has empty size");
      return false;
    }
    return true;
  }
}

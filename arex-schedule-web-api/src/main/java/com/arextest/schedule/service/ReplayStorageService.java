package com.arextest.schedule.service;

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
import java.util.Optional;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReplayStorageService {

  @Resource
  private HttpWepServiceApiClient client;

  @Value("${arex.storage.postProcess.url}")
  private String postProcessUrl;
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

      String out = client.jsonPost(true, postProcessUrl, storageReq, String.class);
      LOGGER.info("postProcessCompareResult result: {}", out);
    } catch (Exception e) {
      LOGGER.error("postProcessCompareResult error", e);
    }
  }
}

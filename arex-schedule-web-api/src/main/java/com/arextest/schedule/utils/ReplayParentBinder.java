package com.arextest.schedule.utils;

import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;

/**
 * @author jmo
 * @since 2021/10/12
 */
public final class ReplayParentBinder {

  private ReplayParentBinder() {

  }

  public static void setupReplayActionParent(List<ReplayActionItem> replayActionItemList,
      ReplayPlan replayPlan) {
    if (CollectionUtils.isEmpty(replayActionItemList)) {
      return;
    }
    for (ReplayActionItem actionItem : replayActionItemList) {
      actionItem.setParent(replayPlan);
    }
  }

  public static void setupCaseItemParent(List<ReplayActionCaseItem> sourceItemList,
      ReplayActionItem actionItem) {
    if (CollectionUtils.isEmpty(sourceItemList)) {
      return;
    }
    for (ReplayActionCaseItem caseItem : sourceItemList) {
      setupCaseItemParent(caseItem, actionItem);
    }
  }

  public static void setupCaseItemParent(ReplayActionCaseItem caseItem,
      ReplayActionItem actionItem) {
    ReplayPlan replayPlan = actionItem.getParent();
    caseItem.setParent(actionItem);
    caseItem.setPlanId(replayPlan.getId());
  }

  public static void setupCaseItemParent(List<ReplayActionCaseItem> sourceItemList,
      ReplayPlan replayPlan) {
    if (CollectionUtils.isEmpty(sourceItemList)) {
      return;
    }
    Map<String, ReplayActionItem> actionItems = replayPlan.getActionItemMap();
    for (ReplayActionCaseItem caseItem : sourceItemList) {
      caseItem.setParent(actionItems.get(caseItem.getPlanItemId()));
      caseItem.setPlanId(replayPlan.getId());
    }
  }
}
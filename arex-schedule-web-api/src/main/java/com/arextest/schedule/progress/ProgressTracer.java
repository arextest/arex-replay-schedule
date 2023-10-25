package com.arextest.schedule.progress;

import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;

/**
 * @author jmo
 * @since 2021/10/11
 */
public interface ProgressTracer {

  void initTotal(ReplayPlan replayPlan);

  void finishOne(ReplayActionCaseItem caseItem);

  default void finishCaseByAction(ReplayActionItem actionItem) {
    finishCaseByAction(actionItem, 1);
  }

  void finishCaseByAction(ReplayActionItem actionItem, int count);


  default void finishCaseByPlan(ReplayPlan replayPlan) {
    finishCaseByPlan(replayPlan, 1);
  }

  void finishCaseByPlan(ReplayPlan replayPlan, int count);


  double finishPercent(String planId);

  long lastUpdateTime(String planId);

  void refreshUpdateTime(String planId);

  void reRunPlan(ReplayPlan replayPlan);
}
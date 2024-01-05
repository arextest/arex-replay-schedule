package com.arextest.schedule.service;

import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class TestBase {

  protected ReplayPlan preparePlan() {
    ReplayPlan plan = new ReplayPlan();
    plan.setCaseCountLimit(1000);
    plan.setCaseSourceFrom(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
    plan.setCaseSourceTo(new Date());
    List<ReplayActionItem> actions = new ArrayList<>();
    ReplayActionItem action1 = new ReplayActionItem();
    action1.setParent(plan);
    actions.add(action1);
    plan.setReplayActionItemList(actions);
    return plan;
  }

  protected ReplayActionCaseItem prepareBaseCase() {
    ReplayActionCaseItem baseCase = new ReplayActionCaseItem();
    baseCase.setId("1");
    return baseCase;
  }
}

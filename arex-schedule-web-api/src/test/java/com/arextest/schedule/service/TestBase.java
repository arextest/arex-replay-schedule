package com.arextest.schedule.service;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

public abstract class TestBase {

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
  }
  protected ReplayPlan preparePlan() {
    ReplayPlan plan = new ReplayPlan();
    plan.setCaseCountLimit(1000);
    plan.setCaseSourceFrom(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
    plan.setCaseSourceTo(new Date());
    List<ReplayActionItem> actions = new ArrayList<>();
    ReplayActionItem action1 = prepareAction();
    action1.setParent(plan);
    actions.add(action1);
    plan.setReplayActionItemList(actions);
    return plan;
  }

  protected ReplayActionItem prepareAction() {
    ReplayActionItem action1 = new ReplayActionItem();
    action1.setId("1");
    return action1;
  }

  protected ReplayActionItem prepareActionWithParent() {
    ReplayActionItem action1 = new ReplayActionItem();
    action1.setId("1");
    ReplayPlan plan = preparePlan();
    action1.setParent(plan);
    return action1;
  }

  protected ReplayActionCaseItem prepareBaseCase() {
    ReplayActionCaseItem baseCase = new ReplayActionCaseItem();
    baseCase.setId("1");
    return baseCase;
  }

  protected ReplayActionCaseItem prepareCaseWithParent() {
    ReplayActionCaseItem baseCase = prepareBaseCase();
    ReplayActionItem parent = new ReplayActionItem();
    ReplayPlan plan = new ReplayPlan();
    parent.setParent(plan);
    baseCase.setParent(parent);
    return baseCase;
  }

  protected AREXMocker prepareMocker() {
    AREXMocker mocker = new AREXMocker();
    mocker.setId("1");
    mocker.setAppId("1");
    mocker.setTargetRequest(new Target());
    mocker.setTargetResponse(new Target());
    mocker.setCategoryType(MockCategoryType.DATABASE);

    return mocker;
  }
}

package com.arextest.schedule.service;

import com.arextest.schedule.comparer.ReplayResultComparer;
import com.arextest.schedule.mdc.AbstractTracedRunnable;
import com.arextest.schedule.model.ReplayActionCaseItem;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wildeslam.
 * @create 2023/11/27 19:36
 */
@Slf4j
public class AsyncCompareCaseTaskRunnable extends AbstractTracedRunnable {

  private final ReplayResultComparer replayResultComparer;

  private final ReplayActionCaseItem caseItem;

  AsyncCompareCaseTaskRunnable(
      ReplayResultComparer replayResultComparer,
      ReplayActionCaseItem caseItem) {
    this.replayResultComparer = replayResultComparer;
    this.caseItem = caseItem;
  }

  @Override
  protected void doWithTracedRunning() {
    boolean compareSuccess = replayResultComparer.compare(caseItem, true);
    if (!compareSuccess) {
      LOGGER.error("Comparer returned false, retry, case id: {}", caseItem.getId());
    }
  }
}

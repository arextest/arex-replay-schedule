package com.arextest.schedule.service;

import com.arextest.common.runnable.AbstractContextWithTraceRunnable;
import com.arextest.schedule.mdc.AbstractTracedRunnable;
import com.arextest.schedule.model.ReplayActionCaseItem;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xinyuan_wang.
 * @create 2024/02/22
 */
@Slf4j
public class AsyncDelayCompareCaseTaskRunnable extends AbstractContextWithTraceRunnable {

  private final ReplayCompareService replayCompareService;

  private final ReplayActionCaseItem caseItem;

  AsyncDelayCompareCaseTaskRunnable(
      ReplayCompareService replayCompareService,
      ReplayActionCaseItem caseItem) {
    this.replayCompareService = replayCompareService;
    this.caseItem = caseItem;
  }


  @Override
  protected void doWithContextRunning() {
    boolean compareSuccess = replayCompareService.compareCaseDistributable(caseItem);
    if (!compareSuccess) {
      LOGGER.error("Comparer returned false, case id: {}", caseItem.getId());
    }
  }
}

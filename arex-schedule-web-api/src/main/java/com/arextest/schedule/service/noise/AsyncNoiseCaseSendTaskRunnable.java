package com.arextest.schedule.service.noise;

import com.arextest.common.runnable.AbstractContextWithTraceRunnable;
import com.arextest.model.replay.CaseSendScene;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.sender.ReplaySender;
import java.util.concurrent.CountDownLatch;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by coryhh on 2023/10/17.
 */
@Slf4j
@Setter
public class AsyncNoiseCaseSendTaskRunnable extends AbstractContextWithTraceRunnable {

  private transient ReplaySender replaySender;
  private CountDownLatch countDownLatch;
  private transient ReplayActionCaseItem caseItem;

  public AsyncNoiseCaseSendTaskRunnable(ReplaySender replaySender,
      CountDownLatch countDownLatch, ReplayActionCaseItem caseItem) {
    this.replaySender = replaySender;
    this.countDownLatch = countDownLatch;
    this.caseItem = caseItem;
  }

  @Override
  protected void doWithContextRunning() {
    boolean success = false;
    try {
      caseItem.setCaseSendScene(CaseSendScene.EXTRA);
      success = replaySender.send(caseItem);
      LOGGER.info("async run sender Id: {} , result:{}", caseItem.getId(), success);
    } catch (RuntimeException exception) {
      caseItem.setSendErrorMessage(exception.getMessage());
      LOGGER.error("failed to send case for noise analysis: {}", caseItem.getId(), exception);
    } finally {
      countDownLatch.countDown();
    }
  }
}

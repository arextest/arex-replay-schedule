package com.arextest.schedule.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.arextest.schedule.mdc.AbstractTracedRunnable;
import com.arextest.schedule.model.ReplayActionCaseItem;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

/**
 * @author xinyuan_wang.
 * @create 2024/02/22
 */
@Slf4j
public class AsyncDelayCompareCaseTaskRunnableTest {

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testDoWithTracedRunningWithCompareSuccess() {
    // Arrange
    ReplayActionCaseItem caseItem = new ReplayActionCaseItem();
    ReplayCompareService replayCompareService = mock(ReplayCompareService.class);
    when(replayCompareService.compareCaseDistributable(caseItem)).thenReturn(true);

    AsyncDelayCompareCaseTaskRunnable runnable = new AsyncDelayCompareCaseTaskRunnable(
        replayCompareService, caseItem);

    // Act
    runnable.doWithContextRunning();

    // Assert
    verify(replayCompareService).compareCaseDistributable(caseItem);
  }

  @Test
  void testDoWithTracedRunningWithCompareFail() {
    // Arrange
    ReplayActionCaseItem caseItem = new ReplayActionCaseItem();
    ReplayCompareService replayCompareService = mock(ReplayCompareService.class);
    when(replayCompareService.compareCaseDistributable(caseItem)).thenReturn(false);

    AsyncDelayCompareCaseTaskRunnable runnable = new AsyncDelayCompareCaseTaskRunnable(
        replayCompareService, caseItem);


    // Act
    runnable.doWithContextRunning();

    // Assert
    verify(replayCompareService).compareCaseDistributable(caseItem);
  }

}

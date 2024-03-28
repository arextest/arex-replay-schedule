package com.arextest.schedule.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.planexecution.PlanMonitorHandler;
import com.arextest.schedule.planexecution.impl.PlanExecutionMonitorImpl;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author xinyuan_wang.
 * @create 2024/02/22
 */
@Slf4j
class PlanExecutionMonitorImplTest {
  @Mock
  private ScheduledExecutorService mockMonitorScheduler;
  @Mock
  private PlanMonitorHandler mockPlanMonitorHandlerA;
  @Mock
  private PlanMonitorHandler mockPlanMonitorHandlerB;
  @InjectMocks
  private PlanExecutionMonitorImpl planExecutionMonitor;
  @BeforeEach
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    planExecutionMonitor = new PlanExecutionMonitorImpl();
    ReflectionTestUtils.setField(planExecutionMonitor, "monitorScheduler", mockMonitorScheduler);
    List<PlanMonitorHandler> handlerList = Arrays.asList(mockPlanMonitorHandlerA, mockPlanMonitorHandlerB);
    ReflectionTestUtils.setField(planExecutionMonitor, "planMonitorHandlerList", handlerList);
  }

  @Test
  void register_shouldScheduleMonitorTaskForEachHandler() {
    ReplayPlan task = new ReplayPlan();
    task.setId("test");
    ScheduledFuture mockMonitorFutureA = mock(ScheduledFuture.class);
    ScheduledFuture mockMonitorFutureB = mock(ScheduledFuture.class);
    when(mockMonitorScheduler.scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(1L), eq(TimeUnit.SECONDS)))
        .thenReturn(mockMonitorFutureA, mockMonitorFutureB);
    ReflectionTestUtils.setField(planExecutionMonitor, "monitorScheduler", mockMonitorScheduler);
    planExecutionMonitor.register(task);
    assertEquals(2, task.getMonitorFutures().size());

  }


  @Test
  void deregister_shouldCancelAllMonitorFuturesAndCallEndMethodOfPlanMonitorHandler() {
    ReplayPlan task = new ReplayPlan();
    task.setId("test");
    ScheduledFuture mockMonitorFutureA = mock(ScheduledFuture.class);
    ScheduledFuture mockMonitorFutureB = mock(ScheduledFuture.class);
    task.setMonitorFutures(Arrays.asList(mockMonitorFutureA, mockMonitorFutureB));
    planExecutionMonitor.deregister(task);
    verify(mockMonitorFutureA, times(1)).cancel(false);
    verify(mockMonitorFutureB, times(1)).cancel(false);
    verify(mockPlanMonitorHandlerA, times(1)).end(task);
    verify(mockPlanMonitorHandlerB, times(1)).end(task);
  }


}

package com.arextest.schedule.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import com.arextest.schedule.model.plan.BuildReplayPlanRequest;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PlanProduceServiceTest {

  @InjectMocks
  private PlanProduceService service;
  @Mock
  private ConfigProvider configProvider;
  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
  }
  @Test
  void testFillOptionalValueIfRequestMissed() {
    // Arrange
    BuildReplayPlanRequest request = new BuildReplayPlanRequest();
    long offsetMillis = 600000L; // 10 minutes in milliseconds
    when(configProvider.getCaseSourceToOffsetMillis()).thenReturn(offsetMillis);

    // Act
    service.fillOptionalValueIfRequestMissed(request);

    // Assert
    assertTrue(request.getCaseSourceTo().before(new Date(System.currentTimeMillis() - offsetMillis))
    || request.getCaseSourceTo().equals(new Date(System.currentTimeMillis() - offsetMillis)));
  }



}

package com.arextest.schedule.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class DefaultConfigProviderImplTest {

  @InjectMocks
  private DefaultConfigProviderImpl defaultConfigProviderImpl;
  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testGetCompareDelaySecondsWithEmptyInput() {
    ReflectionTestUtils.setField(defaultConfigProviderImpl, "delaySeconds", 60);
    int result = defaultConfigProviderImpl.getCompareDelaySeconds("");
    // Assert
    assertEquals(result, 60);
  }


}
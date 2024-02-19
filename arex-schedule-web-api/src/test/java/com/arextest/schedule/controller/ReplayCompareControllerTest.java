package com.arextest.schedule.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.ReplayCompareRequestType;
import com.arextest.schedule.service.ReplayCompareService;
import com.arextest.schedule.web.controller.ReplayCompareController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * created by xinyuan_wang on 2023/11/15
 */
class ReplayCompareControllerTest {
    @Mock
    private ReplayCompareService replayCompareService;

    @InjectMocks
    private ReplayCompareController replayCompareController;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }
    @Test
    void testCompareCase_WhenRequestTypeIsNull_ReturnsBadResponse() {
        ReplayCompareRequestType requestType = null;
        CommonResponse result = replayCompareController.compareCase(requestType);
        assertEquals("requestType is null.", result.getDesc());
    }

    @Test
    void testCompareCase_WhenRequestTypeIsValid_ReturnsSuccessResponse() {
        ReplayCompareRequestType requestType = new ReplayCompareRequestType();
        when(replayCompareService.checkAndCompare(requestType)).thenReturn(true);
        CommonResponse result = replayCompareController.compareCase(requestType);
        assertEquals("success", result.getDesc());
    }
}

package com.arextest.schedule.service;

import static com.arextest.schedule.common.CommonConstant.STOP_PLAN_REDIS_KEY;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.arextest.common.cache.CacheProvider;
import com.arextest.schedule.comparer.ReplayResultComparer;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayCompareRequestType;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ReplayCompareServiceTest {

  @InjectMocks
  private ReplayCompareService replayCompareService;
  @Mock
  private ReplayResultComparer comparer;
  @Mock
  private CacheProvider redisCacheProvider;

  private final String COMPARE_CASE_URL = "http://localhost:8080/api/compareCase";
  private final String PLAN_ID = "123";
  private final String RECORD_ID = "456";
  private final String CASE_ID = "789";
  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
  }
  @Test
  void testCheckAndCompare_WhenPlanCancelled_ReturnTrue() {
    // Arrange
    ReplayActionCaseItem caseItem = new ReplayActionCaseItem();
    caseItem.setPlanId(PLAN_ID);
    when(redisCacheProvider.get((STOP_PLAN_REDIS_KEY + PLAN_ID)
        .getBytes(StandardCharsets.UTF_8))).thenReturn(new byte[1]);
    boolean result = replayCompareService.checkAndCompare(caseItem);
    assertTrue(result);
  }

  @Test
  void testCheckAndCompare_WhenPlanNotCancelled_ReturnTrue() {
    // 构造测试数据
    ReplayActionCaseItem caseItem = new ReplayActionCaseItem();
    caseItem.setRecordId(RECORD_ID);
    caseItem.setPlanId(PLAN_ID);
    caseItem.setId(CASE_ID);
    // 模拟方法调用
    when(redisCacheProvider.get(any(byte[].class))).thenReturn(null);

    // 调用待测试方法
    boolean result = replayCompareService.checkAndCompare(caseItem);

    // 验证结果
    assertTrue(result);
  }

  @Test
  void testGetValue() {
    ReplayCompareRequestType requestType = new ReplayCompareRequestType();
    requestType.setCaseType("testValue");
    Assertions.assertEquals("testValue", requestType.getCaseType());
  }

//  @Test
//  void testCheckAndCompare_WhenSendRequestReturnsTrue_ReturnsTrue() {
//    // 构造测试数据
//    ReplayActionCaseItem caseItem = new ReplayActionCaseItem();
//    caseItem.setRecordId(RECORD_ID);
//    caseItem.setPlanId(PLAN_ID);
//    caseItem.setId(CASE_ID);
//    CommonResponse response = new CommonResponse();
//    response.setData(true);
//    HttpWepServiceApiClient httpWepServiceApiClient = new HttpWepServiceApiClient();
//    when(httpWepServiceApiClient.retryJsonPost(eq(COMPARE_CASE_URL),
//        eq(caseItem), eq(CommonResponse.class))).thenAnswer(
//        invocationOnMock -> {
//          RetryCallback retryCallback = invocationOnMock.getArgument(0);
//          retryCallback.doWithRetry(null);
//          return response;
//        });
//    // 调用待测试方法
//    boolean result = replayCompareService.compareCaseDistributable(caseItem);
//    // 验证结果
//    assertFalse(result);
//  }
//
//  @Test
//  void testCheckAndCompare_WhenSendRequestReturnsFalse_ReturnsFalse() {
//    // 构造测试数据
//    ReplayActionCaseItem caseItem = new ReplayActionCaseItem();
//    caseItem.setRecordId(RECORD_ID);
//    caseItem.setPlanId(PLAN_ID);
//    new Expectations() {{
//      httpWepServiceApiClient.retryJsonPost(COMPARE_CASE_URL, caseItem,
//          CommonResponse.class);
//      result = new CommonResponse();
//    }};
//
//    // 调用待测试方法
//    boolean result = replayCompareService.compareCaseDistributable(caseItem);
//
//    // 验证结果
//    assertFalse(result);
//  }

}

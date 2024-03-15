package com.arextest.schedule.service;

import static com.arextest.schedule.common.CommonConstant.STOP_PLAN_REDIS_KEY;

import com.arextest.common.cache.CacheProvider;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.comparer.ReplayResultComparer;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.ReplayActionCaseItem;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReplayCompareService {

  @Value("${arex.schedule.compare.case.url}")
  private String compareCaseUrl;
  @Resource
  private HttpWepServiceApiClient httpWepServiceApiClient;
  @Resource
  private ReplayResultComparer comparer;
  @Resource
  private CacheProvider redisCacheProvider;

  public boolean checkAndCompare(ReplayActionCaseItem caseItem) {
    if (isCancelled(caseItem.getPlanId())) {
      LOGGER.info("[[title=compareCase]]compare cancelled, recordId: {}, planId: {}",
          caseItem.getRecordId(), caseItem.getPlanId());
      return true;
    }
    LOGGER.info("[[title=compareCase]]compare start, recordId: {}, planId: {}",
        caseItem.getRecordId(), caseItem.getPlanId());
    CompletableFuture.runAsync(() -> comparer.compare(caseItem, true));
    return true;
  }

  public boolean compareCaseDistributable(ReplayActionCaseItem caseItem) {
    try {
      MDCTracer.addDetailId(caseItem.getId());
      CommonResponse response = httpWepServiceApiClient.retryJsonPost(compareCaseUrl, caseItem,
          CommonResponse.class);
      if (response == null || response.getData() == null) {
        LOGGER.warn("[[title=compareCase]]compareCase request recordId: {}, response is null",
            caseItem.getRecordId());
        // if unable to reach the compare service, need to finalize this case locally
        return checkAndCompare(caseItem);
      }
      return (boolean) response.getData();
    } catch (Exception e) {
      LOGGER.error("[[title=compareCase]]failed to compareCase {}, {}", caseItem.getPlanId(), caseItem.getRecordId(), e);
      return false;
    } finally {
      MDCTracer.clear();
    }
  }

  private boolean isCancelled(String planId) {
    try {
      byte[] stopKey = (STOP_PLAN_REDIS_KEY + planId).getBytes(StandardCharsets.UTF_8);
      byte[] bytes = redisCacheProvider.get(stopKey);
      return bytes != null;
    } catch (Exception e) {
      LOGGER.error("check plan id: {} cancelled from redis error", planId, e);
      return false;
    }
  }
}

package com.arextest.schedule.service;

import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.mdc.MDCTracer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReplayRunDetailsService {
  @Resource
  private ReplayActionCaseItemRepository repository;

  /**
   * update case status
   * @param recordId
   * @param caseStatus
   * @return
   */
  public long updateCaseStatus(String recordId, Integer caseStatus) {
    if (StringUtils.isEmpty(recordId) || caseStatus == null) {
      return 0L;
    }

    MDCTracer.addRecordId(recordId);
    try {
      long count = repository.updateCaseStatus(recordId, caseStatus);
      LOGGER.info("updateCaseStatus recordId:{} caseStatus:{} count:{}", recordId, caseStatus, count);
      return count;
    } catch (Exception e) {
      LOGGER.error("updateCaseStatus error", e);
      return 0L;
    } finally {
      MDCTracer.clear();
    }
  }
}

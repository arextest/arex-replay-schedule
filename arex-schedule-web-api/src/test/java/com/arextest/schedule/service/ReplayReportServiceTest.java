package com.arextest.schedule.service;

import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.dao.mongodb.ReplayCompareResultRepositoryImpl;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.model.converter.ReplayCompareResultConverter;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class ReplayReportServiceTest extends TestBase {

  @InjectMocks
  ReplayReportService service;
  @Mock
  private ReplayCompareResultRepositoryImpl replayCompareResultRepository;
  @Mock
  private ReplayCompareResultConverter converter;
  @Mock
  private HttpWepServiceApiClient httpWepServiceApiClient;

  @Test
  void initReportInfo() {
    service.initReportInfo(preparePlan());
  }

  @Test
  void updateReportCaseCount() {
    service.updateReportCaseCount(preparePlan());
  }

  @Test
  void pushPlanStatus() {
    service.pushPlanStatus("XXX", ReplayStatusType.RUNNING, null, false);
    service.pushPlanStatus("XXX", ReplayStatusType.RUNNING, "", false);
    service.pushPlanStatus("XXX", ReplayStatusType.FINISHED, null, false);
    service.pushPlanStatus("XXX", ReplayStatusType.FAIL_INTERRUPTED, null, false);
    service.pushPlanStatus("XXX", ReplayStatusType.CASE_LOADED, null, false);
  }

  @Test
  void write() {
  }

  @Test
  void writeIncomparable() {
    ReplayActionCaseItem caseItem = prepareCaseWithParent();
    service.writeIncomparable(caseItem, "XXX");
    service.writeIncomparable(caseItem, null);
  }

  @Test
  void writeQmqCompareResult() {
    ReplayActionCaseItem caseItem = prepareCaseWithParent();
    service.writeQmqCompareResult(caseItem);
  }

  @Test
  void removeRecordsAndScenes() {
  }

  @Test
  void removeErrorMsg() {
  }

  @Test
  void queryPlanStatistic() {
    service.queryPlanStatistic("TEST", "TEST");
  }
}
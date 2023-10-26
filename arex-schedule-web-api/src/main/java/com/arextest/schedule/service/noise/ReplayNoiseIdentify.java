package com.arextest.schedule.service.noise;

import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.report.QueryNoiseResponseType;
import java.util.List;

/**
 * Created by coryhh on 2023/10/17.
 */
public interface ReplayNoiseIdentify {

  void noiseIdentify(List<ReplayActionCaseItem> allCasesOfContext,
      PlanExecutionContext<?> executionContext);

  void rerunNoiseAnalysisRecovery(List<ReplayActionItem> actionItems);

  QueryNoiseResponseType queryNoise(String planId, String planItemId);
}

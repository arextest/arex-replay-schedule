package com.arextest.schedule.service;

import com.arextest.schedule.model.CaseSendStatusType;
import com.arextest.schedule.model.ReplayActionCaseItem;

/**
 * @author wildeslam.
 * @create 2023/12/1 15:05
 */
public interface ReplayCaseTransmitService {
  void updateSendResult(ReplayActionCaseItem caseItem, CaseSendStatusType sendStatusType);

  void doSendFailedAsFinish(ReplayActionCaseItem caseItem, CaseSendStatusType sendStatusType);
}

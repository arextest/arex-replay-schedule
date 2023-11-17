package com.arextest.schedule.service;

import com.arextest.schedule.model.plan.BuildReplayPlanRequest;
import com.arextest.schedule.model.plan.QueryReplayCaseIdResponse;
import com.arextest.schedule.model.plan.QueryReplayCaseResponse;
import com.arextest.web.model.contract.contracts.QueryReplayCaseRequestType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author wildeslam.
 * @create 2023/11/15 17:40
 */
@Service
@Slf4j
public class LocalReplayService {

  public QueryReplayCaseIdResponse queryReplayCaseId(BuildReplayPlanRequest request) {


    return new QueryReplayCaseIdResponse();
  }

  public QueryReplayCaseResponse queryReplayCase(QueryReplayCaseRequestType replayCaseRequestType) {
    return new QueryReplayCaseResponse();
  }
}

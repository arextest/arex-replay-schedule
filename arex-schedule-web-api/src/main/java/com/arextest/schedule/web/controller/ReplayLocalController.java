package com.arextest.schedule.web.controller;

import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.plan.BuildReplayPlanRequest;
import com.arextest.schedule.service.LocalReplayService;
import com.arextest.web.model.contract.contracts.QueryReplayCaseRequestType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author wildeslam.
 * @create 2023/11/15 17:23
 */
@Slf4j
@Controller
@RequestMapping(path = "/api/replay/local/", produces = {MediaType.APPLICATION_JSON_VALUE})
public class ReplayLocalController {

  @Autowired
  private LocalReplayService localReplayService;

  @PostMapping(value = "/queryCaseId")
  @ResponseBody
  public CommonResponse queryCaseId(@RequestBody BuildReplayPlanRequest request) {
    return CommonResponse.successResponse("success", localReplayService.queryReplayCaseId(request));
  }

  @PostMapping(value = "/queryCases")
  @ResponseBody
  public CommonResponse queryCases(@RequestBody QueryReplayCaseRequestType request) {
    return null;
  }
}

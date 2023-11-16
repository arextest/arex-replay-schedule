package com.arextest.schedule.web.controller;

import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.plan.BuildReplayPlanRequest;
import lombok.extern.slf4j.Slf4j;
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

  @PostMapping(value = "/queryCaseId")
  @ResponseBody
  public CommonResponse queryCaseId(@RequestBody BuildReplayPlanRequest request) {
    return null;
  }

  @PostMapping(value = "/queryCases")
  @ResponseBody
  public CommonResponse queryCases(@RequestBody BuildReplayPlanRequest request) {
    return null;
  }
}

package com.arextest.schedule.web.controller;

import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.report.QueryNoiseResponseType;
import com.arextest.schedule.service.noise.ReplayNoiseIdentify;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
public class ReplayNoiseController {

  @Resource
  ReplayNoiseIdentify replayNoiseIdentify;

  @GetMapping("/api/queryNoise")
  @ResponseBody
  public CommonResponse queryNoise(@RequestParam("planId") String planId, String planItemId) {
    QueryNoiseResponseType queryNoiseResponseType = replayNoiseIdentify.queryNoise(planId,
        planItemId);
    return CommonResponse.successResponse("success", queryNoiseResponseType);
  }

}

package com.arextest.schedule.web.controller;

import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.noiseidentify.ExcludeNoiseRequestType;
import com.arextest.schedule.model.noiseidentify.QueryNoiseResponseType;
import com.arextest.schedule.service.noise.ReplayNoiseHandlerService;
import javax.annotation.Resource;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = "/api", produces = {MediaType.APPLICATION_JSON_VALUE})
public class ReplayNoiseController {

  @Resource
  ReplayNoiseHandlerService replayNoiseHandlerService;

  @GetMapping("/queryNoise")
  public CommonResponse queryNoise(@RequestParam("planId") String planId, String planItemId) {
    QueryNoiseResponseType queryNoiseResponseType = replayNoiseHandlerService.queryNoise(planId,
        planItemId);
    return CommonResponse.successResponse("success", queryNoiseResponseType);
  }

  @PostMapping(value = "/excludeNoise")
  public CommonResponse excludeNoise(@RequestBody @Valid ExcludeNoiseRequestType excludeNoiseRequestType) {
    boolean result = replayNoiseHandlerService.excludeNoise(excludeNoiseRequestType);
    return CommonResponse.successResponse("success", result);
  }

}

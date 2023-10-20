package com.arextest.schedule.web.controller;

import javax.annotation.Resource;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.report.QueryNoiseResponseType;
import com.arextest.schedule.service.noise.ReplayNoiseIdentify;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
public class ReplayNoiseController {

    @Resource
    ReplayNoiseIdentify replayNoiseIdentify;

    @GetMapping("/api/queryNoise")
    @ResponseBody
    public CommonResponse queryNoise(@RequestParam("planId") String planId, String planItemId) {
        QueryNoiseResponseType queryNoiseResponseType = replayNoiseIdentify.queryNoise(planId, planItemId);
        return CommonResponse.successResponse("success", queryNoiseResponseType);
    }

}

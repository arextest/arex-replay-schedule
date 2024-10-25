package com.arextest.schedule.web.controller;

import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.ReplayCompareRequestType;
import com.arextest.schedule.service.ReplayCompareService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * created by xinyuan_wang on 2023/11/15
 */
@Slf4j
@Controller
public class ReplayCompareController {
    @Resource
    private ReplayCompareService replayCompareService;

    @PostMapping("/api/compareCase")
    @ResponseBody
    public CommonResponse compareCase(@RequestBody ReplayCompareRequestType requestType) {
        if (requestType == null) {
            return CommonResponse.badResponse("requestType is null.");
        }
        return CommonResponse.successResponse("success", replayCompareService.checkAndCompare(requestType));
    }



}

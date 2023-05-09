package com.arextest.schedule.web.controller;

import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.DebugRequestItem;
import com.arextest.schedule.model.plan.BuildReplayPlanRequest;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.sender.ReplaySendResult;
import com.arextest.schedule.service.DebugRequestService;
import com.arextest.schedule.service.PlanProduceService;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * @author jmo
 * @since 2021/9/22
 */
@Slf4j
@Controller
@RequestMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
public class ReplayPlanController {
    @Resource
    private PlanProduceService planProduceService;
    @Resource
    private ProgressTracer progressTracer;

    @Resource
    private DebugRequestService debugRequestService;


    @PostMapping(value = "/api/createPlan")
    @ResponseBody
    public CommonResponse createPlanPost(@RequestBody BuildReplayPlanRequest request) {
        return createPlan(request);
    }

    @GetMapping(value = "/api/createPlan")
    @ResponseBody
    public CommonResponse createPlanGet(BuildReplayPlanRequest request) {
        return createPlan(request);
    }

    @GetMapping("/api/stopPlan")
    @ResponseBody
    public CommonResponse stopPlan(String planId) {
        planProduceService.stopPlan(planId);
        return CommonResponse.successResponse("success", null);
    }

    @GetMapping("/progress")
    @ResponseBody
    public CommonResponse progress(String planId) {
        ProgressStatus progressStatus = new ProgressStatus();
        double percent = progressTracer.finishPercent(planId);
        long updateTime = progressTracer.lastUpdateTime(planId);
        progressStatus.setPercent(percent);
        progressStatus.setLastUpdateTime(new Date(updateTime));
        return CommonResponse.successResponse("ok", progressStatus);
    }

    @PostMapping("/api/debugRequest")
    @ResponseBody
    public ReplaySendResult debugRequest(@RequestBody DebugRequestItem requestItem) {
        if (requestItem == null) {
            return ReplaySendResult.failed("param is null");
        }
        if (StringUtils.isBlank(requestItem.getOperation())) {
            return ReplaySendResult.failed("operation is null or empty");
        }
        if (StringUtils.isBlank(requestItem.getMessage())) {
            return ReplaySendResult.failed("message is null or empty");
        }
        return debugRequestService.debugRequest(requestItem);
    }


    @Data
    private static class ProgressStatus {
        double percent;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        Date lastUpdateTime;
    }

    private void fillOptionalValueIfRequestMissed(BuildReplayPlanRequest request) {
        long currentTimeMillis = System.currentTimeMillis();
        Date fromDate = new Date(currentTimeMillis - CommonConstant.ONE_DAY_MILLIS);
        Date toDate = new Date(currentTimeMillis);
        if (request.getCaseSourceFrom() == null) {
            request.setCaseSourceFrom(fromDate);
        }
        if (request.getCaseSourceTo() == null) {
            request.setCaseSourceTo(toDate);
        }
        if (StringUtils.isBlank(request.getPlanName())) {
            request.setPlanName(request.getAppId() + "_" + new SimpleDateFormat("MMdd_HH:mm").format(toDate));
        }
    }

    private CommonResponse createPlan(BuildReplayPlanRequest request) {
        if (request == null) {
            return CommonResponse.badResponse("The request empty not allowed");
        }
        try {
            MDCTracer.addAppId(request.getAppId());
            fillOptionalValueIfRequestMissed(request);
            return planProduceService.createPlan(request);
        } catch (Throwable e) {
            LOGGER.error("create plan error: {} , request: {}", e.getMessage(), request, e);
            return CommonResponse.badResponse("create plan error！" + e.getMessage());
        } finally {
            MDCTracer.clear();
            planProduceService.removeCreating(request.getAppId(), request.getTargetEnv());
        }
    }
}
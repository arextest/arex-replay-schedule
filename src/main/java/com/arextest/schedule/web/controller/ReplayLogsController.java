package com.arextest.schedule.web.controller;

import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.DebugRequestItem;
import com.arextest.schedule.model.bizlog.QueryReplayBizLogsRequest;
import com.arextest.schedule.model.bizlog.QueryReplayBizLogsResponse;
import com.arextest.schedule.model.bizlog.ReplayBizLogQueryCondition;
import com.arextest.schedule.model.dao.mongodb.ReplayBizLogCollection;
import com.arextest.schedule.model.plan.BuildReplayPlanRequest;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.sender.ReplaySendResult;
import com.arextest.schedule.service.DebugRequestService;
import com.arextest.schedule.service.PlanBizLogService;
import com.arextest.schedule.service.PlanProduceService;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author qzmo
 * @since 2023/06/16
 */
@Slf4j
@Controller
@RequestMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
public class ReplayLogsController {
    @Resource
    private PlanBizLogService planBizLogService;

    @GetMapping("/api/queryPlanLogs/{planId}")
    @ResponseBody
    public CommonResponse queryPlanLogs(@PathVariable("planId") String planId) {
        List<ReplayBizLogCollection> logs = planBizLogService.queryBizLogsByPlanId(planId);
        return CommonResponse.successResponse("success", logs);
    }

    @PostMapping("/api/queryPlanLogs")
    @ResponseBody
    public CommonResponse queryPlanLogsPaginated(@RequestBody QueryReplayBizLogsRequest request) {
        QueryReplayBizLogsResponse response = new QueryReplayBizLogsResponse();
        if (request.getCondition() == null) {
            request.setCondition(new ReplayBizLogQueryCondition());
        }

        request.getCondition().validate();

        try {
            response.setLogs(planBizLogService.queryBizLogsByPlanId(request.getPlanId(), request.getCondition()));
            response.setTotal(planBizLogService.countBizLogsByPlanId(request.getPlanId(), request.getCondition()));
            response.setPlanId(request.getPlanId());
        } catch (Exception e) {
            LOGGER.error("Query biz logs error: ", e);
            return CommonResponse.successResponse(e.getMessage(), response);
        }
        return CommonResponse.successResponse("success", response);
    }

}
package com.arextest.schedule.web.controller;

import com.arextest.common.model.response.Response;
import com.arextest.common.utils.ResponseUtils;
import com.arextest.schedule.model.report.QueryDiffMsgByIdResponseType;
import com.arextest.schedule.service.report.QueryReplayMsgService;
import com.arextest.web.model.contract.contracts.QueryLogEntityRequestTye;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * @author qzmo
 * @since 2023/06/19
 */
@Slf4j
@Controller
@RequestMapping("/api/report/")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ReplayReportController {

    @Resource
    private QueryReplayMsgService queryReplayMsgService;

    @GetMapping("/queryDiffMsgById/{id}")
    @ResponseBody
    public Response queryDiffMsgById(@PathVariable String id) {
        QueryDiffMsgByIdResponseType response = queryReplayMsgService.queryDiffMsgById(id);
        return ResponseUtils.successResponse(response);
    }

    @PostMapping("/queryLogEntity")
    @ResponseBody
    public Response queryLogEntity(@Valid @RequestBody QueryLogEntityRequestTye request) {
        return ResponseUtils.successResponse(
                queryReplayMsgService.queryLogEntity(request)
        );
    }
}
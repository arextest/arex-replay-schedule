package com.arextest.schedule.web.controller;

import com.arextest.common.annotation.AppAuth;
import com.arextest.common.enums.AuthRejectStrategy;
import com.arextest.common.model.response.Response;
import com.arextest.common.utils.ResponseUtils;
import com.arextest.schedule.model.report.QueryLogEntityRequestTye;
import com.arextest.schedule.service.report.QueryReplayMsgService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author qzmo
 * @since 2023/06/19
 */
@Slf4j
@Controller
@RequestMapping(path = "/api/report/", produces = {MediaType.APPLICATION_JSON_VALUE})
@CrossOrigin(origins = "*", maxAge = 3600)
public class ReplayReportController {

  @Resource
  private QueryReplayMsgService queryReplayMsgService;

  @ResponseBody
  @GetMapping("/queryDiffMsgById/{id}")
  @AppAuth(rejectStrategy = AuthRejectStrategy.DOWNGRADE)
  public Response queryDiffMsgById(@PathVariable String id) {
    return ResponseUtils.successResponse(queryReplayMsgService.queryDiffMsgById(id));
  }

  @PostMapping("/queryLogEntity")
  @ResponseBody
  public Response queryLogEntity(@Valid @RequestBody QueryLogEntityRequestTye request) {
    return ResponseUtils.successResponse(queryReplayMsgService.queryLogEntity(request));
  }
}
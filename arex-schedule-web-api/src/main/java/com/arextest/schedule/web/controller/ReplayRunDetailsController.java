package com.arextest.schedule.web.controller;

import com.arextest.common.model.response.Response;
import com.arextest.common.utils.ResponseUtils;
import com.arextest.model.replay.UpdateCaseStatusRequestType;
import com.arextest.schedule.service.ReplayRunDetailsService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author xinyuan_wang
 * @since 2024/7/16
 */
@Slf4j
@Controller
@RequestMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
public class ReplayRunDetailsController {
  @Resource
  private ReplayRunDetailsService replayRunDetailsService;

  @PostMapping("/api/updateCaseStatus")
  @ResponseBody
  public Response updateCaseStatus(@RequestBody @Valid UpdateCaseStatusRequestType requestType) {
    return ResponseUtils.successResponse(replayRunDetailsService.updateCaseStatus(
            requestType.getRecordId(), requestType.getCaseStatus()));
  }
}
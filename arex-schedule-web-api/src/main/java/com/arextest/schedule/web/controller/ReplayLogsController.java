package com.arextest.schedule.web.controller;

import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.bizlog.QueryReplayBizLogsRequest;
import com.arextest.schedule.model.bizlog.QueryReplayBizLogsResponse;
import com.arextest.schedule.model.bizlog.ReplayBizLogQueryCondition;
import com.arextest.schedule.model.dao.mongodb.ReplayBizLogCollection;
import com.arextest.schedule.service.PlanBizLogService;
import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
      response.setLogs(
          planBizLogService.queryBizLogsByPlanId(request.getPlanId(), request.getCondition()));
      response.setTotal(
          planBizLogService.countBizLogsByPlanId(request.getPlanId(), request.getCondition()));
      response.setPlanId(request.getPlanId());
    } catch (Exception e) {
      LOGGER.error("Query biz logs error: ", e);
      return CommonResponse.successResponse(e.getMessage(), response);
    }
    return CommonResponse.successResponse("success", response);
  }

}
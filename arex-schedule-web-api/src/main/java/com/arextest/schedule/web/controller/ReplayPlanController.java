package com.arextest.schedule.web.controller;

import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.exceptions.PlanRunningException;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.plan.BuildReplayFailReasonEnum;
import com.arextest.schedule.model.plan.BuildReplayPlanRequest;
import com.arextest.schedule.model.plan.BuildReplayPlanResponse;
import com.arextest.schedule.model.plan.BuildReplayPlanType;
import com.arextest.schedule.model.plan.OperationCaseInfo;
import com.arextest.schedule.model.plan.ReRunReplayPlanRequest;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.service.PlanProduceService;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
  private ProgressEvent progressEvent;
  @Resource
  private ObjectMapper objectMapper;
  private static final TypeReference<Map<String, String>> TAGS_TYPE = new TypeReference<Map<String, String>>() {
  };

  @PostMapping(value = "/api/createPlan")
  @ResponseBody
  public CommonResponse createPlanPost(@RequestBody BuildReplayPlanRequest request) {
    return createPlan(request);
  }

  @GetMapping(value = "/api/createPlan")
  @ResponseBody
  public CommonResponse createPlanGet(@RequestParam(name = "appId") String appId,
      @RequestParam(name = "targetEnv") String targetEnv,
      @RequestParam(name = "caseSourceFrom", required = false) Long caseSourceFrom,
      @RequestParam(name = "caseSourceTo", required = false) Long caseSourceTo,
      @RequestParam(name = "caseCountLimit", required = false) Integer caseCountLimit,
      @RequestParam(name = "planName", required = false) String planName,
      @RequestParam(name = "operationIds", required = false) List<String> operationIds,
      @RequestParam(name = "caseTags", required = false) String caseTags
  ) {
    BuildReplayPlanRequest req = new BuildReplayPlanRequest();
    req.setAppId(appId);
    req.setTargetEnv(targetEnv);

    req.setReplayPlanType(BuildReplayPlanType.BY_APP_ID.getValue());
    req.setOperator("Webhook");
    req.setPlanName(planName);
    req.setCaseCountLimit(caseCountLimit);

    // date
    Date dateFrom = Optional.ofNullable(caseSourceFrom).map(Date::new)
        .orElse(new Date(System.currentTimeMillis() - CommonConstant.ONE_DAY_MILLIS));
    req.setCaseSourceFrom(dateFrom);
    Date dateTo = Optional.ofNullable(caseSourceTo).map(Date::new).orElse(new Date());
    req.setCaseSourceTo(dateTo);

    // operation filter
    if (!CollectionUtils.isEmpty(operationIds)) {
      req.setOperationCaseInfoList(operationIds.stream().map(operationId -> {
        OperationCaseInfo operationCaseInfo = new OperationCaseInfo();
        operationCaseInfo.setOperationId(operationId);
        return operationCaseInfo;
      }).collect(Collectors.toList()));
      req.setReplayPlanType(BuildReplayPlanType.BY_OPERATION_OF_APP_ID.getValue());
    }

    if (StringUtils.isNotEmpty(caseTags)) {
      try {
        req.setCaseTags(objectMapper.readValue(caseTags, TAGS_TYPE));
      } catch (Exception e) {
        LOGGER.error("parse caseTags error", e);
      }
    }
    return createPlan(req);
  }

  @PostMapping("/api/reRunPlan")
  @ResponseBody
  public CommonResponse reRunPlan(@RequestBody ReRunReplayPlanRequest request) {
    try {
      MDCTracer.addPlanId(request.getPlanId());
      MDCTracer.addPlanItemId(request.getPlanItemId());
      return planProduceService.reRunPlan(request);
    } catch (PlanRunningException e) {
      return CommonResponse.badResponse(e.getMessage(),
          new BuildReplayPlanResponse(e.getCode()));
    } catch (Throwable e) {
      return CommonResponse.badResponse("create plan error！" + e.getMessage(),
          new BuildReplayPlanResponse(BuildReplayFailReasonEnum.UNKNOWN));
    } finally {
      MDCTracer.clear();
    }
  }

  @GetMapping("/api/stopPlan")
  @ResponseBody
  public CommonResponse stopPlan(String planId, String operator) {
    planProduceService.stopPlan(planId, operator);
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

  private CommonResponse createPlan(BuildReplayPlanRequest request) {
    if (request == null) {
      return CommonResponse.badResponse("The request empty not allowed");
    }
    try {
      MDCTracer.addAppId(request.getAppId());
      return planProduceService.createPlan(request);
    } catch (Throwable e) {
      LOGGER.error("create plan error: {} , request: {}", e.getMessage(), request, e);
      progressEvent.onReplayPlanCreateException(request, e);

      if (e instanceof PlanRunningException) {
        PlanRunningException PlanRunningException = (PlanRunningException) e;
        return CommonResponse.badResponse(PlanRunningException.getMessage(),
            new BuildReplayPlanResponse(PlanRunningException.getCode()));
      } else {
        return CommonResponse.badResponse("create plan error！" + e.getMessage(),
            new BuildReplayPlanResponse(BuildReplayFailReasonEnum.UNKNOWN));
      }

    } finally {
      MDCTracer.clear();
      planProduceService.removeCreating(request.getAppId(), request.getTargetEnv());
    }
  }

  @Data
  private static class ProgressStatus {

    double percent;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    Date lastUpdateTime;
  }
}
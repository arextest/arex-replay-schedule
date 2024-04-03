package com.arextest.schedule.web.controller;

import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.plan.BuildReplayFailReasonEnum;
import com.arextest.schedule.model.plan.BuildReplayPlanRequest;
import com.arextest.schedule.model.plan.BuildReplayPlanResponse;
import com.arextest.schedule.model.plan.PostSendRequest;
import com.arextest.schedule.model.plan.PreSendRequest;
import com.arextest.schedule.model.plan.QueryReplaySenderParametersRequest;
import com.arextest.schedule.model.plan.QueryReplaySenderParametersResponse;
import com.arextest.schedule.service.LocalReplayService;
import com.arextest.schedule.service.PlanProduceService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.annotation.Resource;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author wildeslam.
 * @create 2023/11/15 17:23
 */
@Slf4j
@Controller
@RequestMapping(path = "/api/replay/local/", produces = {MediaType.APPLICATION_JSON_VALUE})
public class ReplayLocalController {

  private static final String SUCCESS_DESC = "success";

  @Resource
  private LocalReplayService localReplayService;
  @Resource
  private PlanProduceService planProduceService;

  @Resource(name = "custom-fork-join-executor")
  private ExecutorService customForkJoinExecutor;

  @PostMapping(value = "/queryCaseId")
  @ResponseBody
  public CommonResponse queryCaseId(@Valid @RequestBody BuildReplayPlanRequest request) {
    try {
      planProduceService.fillOptionalValueIfRequestMissed(request);
      return localReplayService.queryReplayCaseId(request);
    } catch (Exception e) {
      LOGGER.error("queryCaseId error: {} , request: {}", e.getMessage(), request, e);
      return CommonResponse.badResponse("queryCaseId error！" + e.getMessage(),
          new BuildReplayPlanResponse(BuildReplayFailReasonEnum.UNKNOWN));
    } finally {
      planProduceService.removeCreating(request.getAppId(), request.getTargetEnv());
    }
  }

  @PostMapping(value = "/queryReplaySenderParameters")
  @ResponseBody
  public CommonResponse queryCases(@Valid @RequestBody QueryReplaySenderParametersRequest request) {
    if (CollectionUtils.isEmpty(request.getCaseIds())) {
      return CommonResponse.badResponse("No caseId!");
    }
    QueryReplaySenderParametersResponse response = localReplayService.queryReplaySenderParameters(
        request);
    return CommonResponse.successResponse(SUCCESS_DESC, response);
  }

  @PostMapping(value = "/preSend")
  @ResponseBody
  public CommonResponse preSend(@Valid @RequestBody PreSendRequest request) {
    boolean success = localReplayService.preSend(request);
    if (success) {
      return CommonResponse.successResponse(SUCCESS_DESC, success);
    } else {
      return CommonResponse.badResponse("SendLimiter break!", success);
    }
  }

  @PostMapping(value = "/postSend")
  @ResponseBody
  public CommonResponse postSend(@Valid @RequestBody PostSendRequest request) {
    CompletableFuture.runAsync(() -> localReplayService.postSend(request), customForkJoinExecutor);
    return CommonResponse.successResponse(SUCCESS_DESC, true);
  }
}

package com.arextest.schedule.service;

import com.arextest.common.cache.CacheProvider;
import com.arextest.common.utils.CompressionUtils;
import com.arextest.model.constants.MockAttributeNames;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.common.JsonUtils;
import com.arextest.schedule.common.SendLimiter;
import com.arextest.schedule.common.SendRedisLimiter;
import com.arextest.schedule.comparer.CompareConfigService;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanActionRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanRepository;
import com.arextest.schedule.model.CaseSendStatusType;
import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayActionItemForCache;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.ReplayPlanForCache;
import com.arextest.schedule.model.ReplayStatusType;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.model.plan.BuildReplayFailReasonEnum;
import com.arextest.schedule.model.plan.BuildReplayPlanRequest;
import com.arextest.schedule.model.plan.BuildReplayPlanResponse;
import com.arextest.schedule.model.plan.PostSendRequest;
import com.arextest.schedule.model.plan.PreSendRequest;
import com.arextest.schedule.model.plan.QueryReplayCaseIdResponse;
import com.arextest.schedule.model.plan.QueryReplaySenderParametersRequest;
import com.arextest.schedule.model.plan.QueryReplaySenderParametersResponse;
import com.arextest.schedule.model.plan.ReRunReplayPlanRequest;
import com.arextest.schedule.model.plan.ReplayCaseBatchInfo;
import com.arextest.schedule.plan.PlanContext;
import com.arextest.schedule.plan.PlanContextCreator;
import com.arextest.schedule.plan.builder.BuildPlanValidateResult;
import com.arextest.schedule.plan.builder.ReplayPlanBuilder;
import com.arextest.schedule.planexecution.PlanExecutionContextProvider;
import com.arextest.schedule.planexecution.PlanExecutionMonitor;
import com.arextest.schedule.planexecution.impl.DefaultExecutionContextProvider.ContextDependenciesHolder;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.sender.ReplaySenderParameters;
import com.arextest.schedule.sender.impl.MockCachePreLoader;
import com.arextest.schedule.utils.DecodeUtils;
import com.arextest.schedule.utils.ReplayParentBinder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

/**
 * @author wildeslam.
 * @create 2023/11/15 17:40
 */
@Service
@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class LocalReplayService {

  private static final String REPLAY_ACTION_ITEM_KEY_FORMAT = "replay_action_item_%s";
  private static final String REPLAY_PLAN_RERUN_KEY_FORMAT = "replay_plan_rerun_%s";

  @Resource
  private PlanProduceService planProduceService;
  @Resource
  private ReplayActionCaseItemRepository replayActionCaseItemRepository;
  @Resource
  private PlanContextCreator planContextCreator;
  @Resource
  private ReplayPlanRepository replayPlanRepository;
  @Resource
  private ReplayPlanActionRepository replayPlanActionRepository;
  @Resource
  private PlanConsumePrepareService planConsumePrepareService;
  @Resource
  private PlanExecutionContextProvider planExecutionContextProvider;
  @Resource
  private ProgressEvent progressEvent;
  @Resource
  private ProgressTracer progressTracer;
  @Resource
  private CacheProvider redisCacheProvider;
  @Resource
  private MockCachePreLoader mockCachePreLoader;
  @Resource
  private ReplayCaseTransmitService replayCaseTransmitService;
  @Resource
  private CompareConfigService compareConfigService;
  @Resource
  private ExecutorService postSendExecutorService;
  @Resource
  private PlanExecutionMonitor planExecutionMonitorImpl;

  public CommonResponse queryReplayCaseId(BuildReplayPlanRequest request) {
    final QueryReplayCaseIdResponse response = new QueryReplayCaseIdResponse();
    Pair<ReplayPlan, CommonResponse> pair = buildReplayPlan(request);
    if (pair.getLeft() == null) {
      return pair.getRight();
    }
    ReplayPlan replayPlan = pair.getLeft();
    response.setPlanId(replayPlan.getId());
    response.setReplayCaseBatchInfos(buildBatchInfoList(replayPlan));
    return CommonResponse.successResponse("queryReplayCaseId success!", response);
  }

  public QueryReplaySenderParametersResponse queryReplaySenderParameters(
      QueryReplaySenderParametersRequest request) {
    QueryReplaySenderParametersResponse response = new QueryReplaySenderParametersResponse();

    List<ReplayActionCaseItem> caseItemList = replayActionCaseItemRepository.batchQueryById(
        request.getCaseIds());
    Map<String, ReplayActionItemForCache> planItemMap = new HashMap<>();
    Map<String, String> replaySenderParametersMap = new HashMap<>();
    for (ReplayActionCaseItem caseItem : caseItemList) {
      String planItemId = caseItem.getPlanItemId();
      ReplayActionItemForCache replayActionItemForCache = planItemMap.getOrDefault(planItemId,
          loadReplayActionItemCache(planItemId));
      if (replayActionItemForCache == null) {
        LOGGER.error("loadReplayActionItemCache failed, planItemId:{}", planItemId);
        continue;
      }
      if (!planItemMap.containsKey(planItemId)) {
        planItemMap.put(planItemId, replayActionItemForCache);
      }
      ReplaySenderParameters senderParameters = buildReplaySenderParameters(caseItem,
          replayActionItemForCache);
      replaySenderParametersMap.put(caseItem.getId(), compress(senderParameters));
    }
    response.setReplaySenderParametersMap(replaySenderParametersMap);
    return response;
  }

  public boolean preSend(PreSendRequest request) {
    ReplayPlan replayPlan = replayPlanRepository.query(request.getPlanId());
    restorePlanCache(replayPlan);

    SendLimiter sendLimiter = new SendRedisLimiter(replayPlan, redisCacheProvider);
    ReplayActionCaseItem caseItem = restoreCase(request.getCaseId(), null);
    if (caseItem != null) {
      if (sendLimiter.failBreak()) {
        replayCaseTransmitService.updateSendResult(caseItem, CaseSendStatusType.EXCEPTION_FAILED);
        return false;
      }
      if (isStop(request.getPlanId())) {
        replayCaseTransmitService.updateSendResult(caseItem, CaseSendStatusType.CANCELED);
        return false;
      }
    }
    return mockCachePreLoader.prepareCache(caseItem);
  }


  public boolean postSend(PostSendRequest request) {
    CompletableFuture.runAsync(() -> postSend0(request), postSendExecutorService);
    return true;
  }

  public CommonResponse queryReRunCaseId(ReRunReplayPlanRequest request) {
    final String planId = request.getPlanId();
    final QueryReplayCaseIdResponse response = new QueryReplayCaseIdResponse();

    ReplayPlan replayPlan = replayPlanRepository.query(planId);


    List<ReplayActionCaseItem> failedCaseList = replayActionCaseItemRepository.failedCaseList(
        planId, request.getPlanItemId());

    if (CollectionUtils.isEmpty(failedCaseList)) {
      progressEvent.onReplayPlanReRunException(replayPlan);
      return CommonResponse.badResponse("No failed case found");
    }

    if (planProduceService.isRunning(planId)) {
      progressEvent.onReplayPlanReRunException(replayPlan);
      return CommonResponse.badResponse("This plan is Running");
    }
    replayPlan.setReRun(Boolean.TRUE);
    cacheReplayPlan(replayPlan);

    planExecutionMonitorImpl.register(replayPlan);
    progressEvent.onReplayPlanReRun(replayPlan);
    progressEvent.onUpdateFailedCases(replayPlan, failedCaseList);
    planConsumePrepareService.updateFailedActionAndCase(replayPlan, failedCaseList);
    if (CollectionUtils.isEmpty(replayPlan.getReplayActionItemList())) {
      throw new RuntimeException("no replayActionItem!");
    }
    replayPlan.setExecutionContexts(planExecutionContextProvider.buildContext(replayPlan));

    response.setReplayCaseBatchInfos(buildBatchInfoList(replayPlan));

    return CommonResponse.successResponse("queryReRunCaseIds success!", response);
  }

  private void postSend0(PostSendRequest request) {
    ReplayPlan replayPlan = replayPlanRepository.query(request.getPlanId());
    restorePlanCache(replayPlan);

    SendLimiter sendLimiter = new SendRedisLimiter(replayPlan, redisCacheProvider);
    sendLimiter.release(request.getSendStatusType() == CaseSendStatusType.SUCCESS.getValue());

    ReplayActionCaseItem caseItem = restoreCase(request.getCaseId(), request.getReplayId());

    replayCaseTransmitService.updateSendResult(caseItem,
        CaseSendStatusType.of(request.getSendStatusType()));
  }

  private ReplayActionCaseItem restoreCase(String caseId, String replayId) {
    ReplayActionCaseItem caseItem = replayActionCaseItemRepository.queryById(caseId);
    caseItem.setTargetResultId(replayId);
    caseItem.setSourceResultId(StringUtils.EMPTY);
    ReplayActionItemForCache replayActionItemForCache = loadReplayActionItemCache(
        caseItem.getPlanItemId());
    if (replayActionItemForCache == null) {
      LOGGER.error("loadReplayActionItemCache failed, planItemId:{}", caseItem.getPlanItemId());
      return null;
    }
    ReplayActionItem replayActionItem = transformFromCache(replayActionItemForCache);
    replayActionItem.setParent(replayPlanRepository.query(caseItem.getPlanId()));
    caseItem.setParent(replayActionItem);
    return caseItem;
  }

  private void cacheReplayActionItem(List<ReplayActionItem> replayActionItemList,
      Set<String> planItemIds) {
    for (ReplayActionItem replayActionItem : replayActionItemList) {
      if (!planItemIds.contains(replayActionItem.getId())) {
        continue;
      }
      ReplayActionItemForCache replayActionItemForCache = transformForCache(replayActionItem);
      redisCacheProvider.put(buildReplayActionItemRedisKey(replayActionItemForCache.getId()),
          CommonConstant.ONE_HOUR_MILLIS,
          JsonUtils.objectToJsonString(replayActionItemForCache).getBytes(StandardCharsets.UTF_8));
    }
  }

  private void cacheReplayPlan(ReplayPlan replayPlan) {
    ReplayPlanForCache replayPlanForCache = transformForCache(replayPlan);
    redisCacheProvider.put(buildReplayPlanRerunRedisKey(replayPlan.getId()),
        CommonConstant.ONE_HOUR_MILLIS,
        JsonUtils.objectToJsonString(replayPlanForCache).getBytes(StandardCharsets.UTF_8));
  }

  private ReplayPlanForCache loadReplayPlanCache(String planId) {
    try {
      byte[] json = doWithRetry(
          () -> redisCacheProvider.get(buildReplayPlanRerunRedisKey(planId)));
      if (json == null) {
        return null;
      }
      return JsonUtils.byteToObject(json, ReplayPlanForCache.class);
    } catch (Throwable e) {
      LOGGER.error("loadReplayPlanCache failed, planId:{}", planId, e);
      return null;
    }
  }

  private byte[] doWithRetry(Supplier<byte[]> action) {
    try {
      return action.get();
    } catch (Throwable throwable) {
      LOGGER.error("do doWithRetry error: {}", throwable.getMessage(), throwable);
      return action.get();
    }
  }

  private ReplayActionItemForCache loadReplayActionItemCache(String planItemId) {
    try {
      byte[] json = doWithRetry(
          () -> redisCacheProvider.get(buildReplayActionItemRedisKey(planItemId)));
      if (json == null) {
        return null;
      }
      return JsonUtils.byteToObject(json, ReplayActionItemForCache.class);
    } catch (Throwable e) {
      LOGGER.error("loadReplayActionItemCache failed, planItemId:{}", planItemId, e);
      return null;
    }
  }

  private ReplayActionItemForCache transformForCache(ReplayActionItem replayActionItem) {
    ReplayActionItemForCache result = new ReplayActionItemForCache();
    result.setId(replayActionItem.getId());
    result.setOperationId(replayActionItem.getOperationId());
    result.setOperationName(replayActionItem.getOperationName());
    result.setServiceKey(replayActionItem.getServiceKey());
    result.setPlanId(replayActionItem.getPlanId());
    result.setAppId(replayActionItem.getAppId());
    result.setTargetInstance(replayActionItem.getTargetInstance());
    result.setExclusionOperationConfig(replayActionItem.getExclusionOperationConfig());
    return result;
  }

  private ReplayActionItem transformFromCache(ReplayActionItemForCache cache) {
    ReplayActionItem result = new ReplayActionItem();
    result.setId(cache.getId());
    result.setOperationId(cache.getOperationId());
    result.setOperationName(cache.getOperationName());
    result.setServiceKey(cache.getServiceKey());
    result.setPlanId(cache.getPlanId());
    result.setAppId(cache.getAppId());
    result.setTargetInstance(cache.getTargetInstance());
    result.setExclusionOperationConfig(cache.getExclusionOperationConfig());
    return result;
  }

  private ReplayPlanForCache transformForCache(ReplayPlan replayPlan) {
    ReplayPlanForCache result = new ReplayPlanForCache();
    result.setId(replayPlan.getId());
    result.setRerun(replayPlan.isReRun());
    return result;
  }

  private byte[] buildReplayActionItemRedisKey(String planItemId) {
    return (String.format(REPLAY_ACTION_ITEM_KEY_FORMAT, planItemId)).getBytes(
        StandardCharsets.UTF_8);
  }

  private byte[] buildReplayPlanRerunRedisKey(String planId) {
    return (String.format(REPLAY_PLAN_RERUN_KEY_FORMAT, planId)).getBytes(
        StandardCharsets.UTF_8);
  }

  private void restorePlanCache(ReplayPlan replayPlan) {
    ReplayPlanForCache replayPlanForCache = loadReplayPlanCache(replayPlan.getId());
    if (replayPlanForCache == null) {
      return;
    }
    replayPlan.setReRun(replayPlanForCache.isRerun());
    replayPlan.setCaseRerunCount(replayPlanForCache.getCaseRerunCount());
  }

  private String compress(ReplaySenderParameters senderParameter) {
    String json = JsonUtils.objectToJsonString(senderParameter);
    if (StringUtils.isEmpty(json)) {
      return StringUtils.EMPTY;
    }
    return CompressionUtils.useZstdCompress(json);
  }

  private ReplaySenderParameters buildReplaySenderParameters(ReplayActionCaseItem caseItem,
      ReplayActionItemForCache replayActionItem) {
    ReplaySenderParameters senderParameter = new ReplaySenderParameters();
    senderParameter.setAppId(replayActionItem.getAppId());
    senderParameter.setConsumeGroup(caseItem.consumeGroup());
    byte[] decodeMessage = (byte[]) DecodeUtils.decode(caseItem.requestMessage());
    String stringMessage = new String(decodeMessage, StandardCharsets.UTF_8);
    senderParameter.setMessage(stringMessage);
    String operationName = caseItem.requestPath();
    if (StringUtils.isEmpty(operationName)) {
      operationName = replayActionItem.getOperationName();
    }
    senderParameter.setOperation(operationName);
    Map<String, String> headers = caseItem.requestHeaders();
    if (headers == null) {
      headers = new HashMap<>();
    }
    headers.put(CommonConstant.AREX_REPLAY_WARM_UP, Boolean.TRUE.toString());
    headers.put(CommonConstant.AREX_RECORD_ID, caseItem.getRecordId());
    ServiceInstance instanceRunner = selectLoadBalanceInstance(caseItem.getId(),
        replayActionItem.getTargetInstance());
    if (instanceRunner == null) {
      LOGGER.error("selectLoadBalanceInstance failed, caseItem:{}", caseItem);
      return null;
    }
    senderParameter.setFormat(headers.get(MockAttributeNames.CONTENT_TYPE));
    senderParameter.setUrl(instanceRunner.getUrl());
    senderParameter.setSubEnv(instanceRunner.subEnv());
    senderParameter.setHeaders(headers);
    senderParameter.setMethod(caseItem.requestMethod());
    senderParameter.setRecordId(caseItem.getRecordId());
    return senderParameter;
  }

  private ServiceInstance selectLoadBalanceInstance(String caseItemId,
      List<ServiceInstance> serviceInstances) {
    if (CollectionUtils.isEmpty(serviceInstances)) {
      return null;
    }
    int index = Math.abs(caseItemId.hashCode() % serviceInstances.size());
    return serviceInstances.get(index);
  }

  private Pair<ReplayPlan, CommonResponse> buildReplayPlan(BuildReplayPlanRequest request) {
    long planCreateMillis = System.currentTimeMillis();
    String appId = request.getAppId();
    if (planProduceService.isCreating(appId, request.getTargetEnv())) {
      return Pair.of(null, CommonResponse.badResponse("This appid is creating plan",
          new BuildReplayPlanResponse(BuildReplayFailReasonEnum.CREATING)));
    }
    ReplayPlanBuilder planBuilder = planProduceService.select(request);
    if (planBuilder == null) {
      return Pair.of(null, CommonResponse.badResponse(
          "appId:" + appId + " unsupported replay planType : " + request.getReplayPlanType(),
          new BuildReplayPlanResponse(BuildReplayFailReasonEnum.INVALID_REPLAY_TYPE)));
    }
    PlanContext planContext = planContextCreator.createByAppId(appId);
    BuildPlanValidateResult result = planBuilder.validate(request, planContext);
    if (result.failure()) {
      return Pair.of(null,
          CommonResponse.badResponse("appId:" + appId + " error: " + result.getRemark(),
              new BuildReplayPlanResponse(planProduceService.validateToResultReason(result))));
    }

    List<ReplayActionItem> replayActionItemList = planBuilder.buildReplayActionList(request,
        planContext);
    if (CollectionUtils.isEmpty(replayActionItemList)) {
      return Pair.of(null,
          CommonResponse.badResponse("appId:" + appId + " error: empty replay actions",
              new BuildReplayPlanResponse(BuildReplayFailReasonEnum.NO_INTERFACE_FOUND)));
    }

    ReplayPlan replayPlan = planProduceService.build(request, planContext);
    replayPlan.setPlanCreateMillis(planCreateMillis);
    replayPlan.setReplayActionItemList(replayActionItemList);
    ReplayParentBinder.setupReplayActionParent(replayActionItemList, replayPlan);

    if (!replayPlanRepository.save(replayPlan)) {
      return Pair.of(null, CommonResponse.badResponse("save replan plan error, " + replayPlan,
          new BuildReplayPlanResponse(BuildReplayFailReasonEnum.DB_ERROR)));
    }
    planProduceService.isRunning(replayPlan.getId());
    if (!replayPlanActionRepository.save(replayActionItemList)) {
      return Pair.of(null, CommonResponse.badResponse("save replay action error, " + replayPlan,
          new BuildReplayPlanResponse(BuildReplayFailReasonEnum.DB_ERROR)));
    }
    progressEvent.onReplayPlanCreated(replayPlan);

    planConsumePrepareService.preparePlan(replayPlan);
    replayPlan.setExecutionContexts(planExecutionContextProvider.buildContext(replayPlan));
    if (CollectionUtils.isEmpty(replayPlan.getExecutionContexts())) {
      replayPlan.setErrorMessage("Got empty execution context");
      progressEvent.onReplayPlanInterrupt(replayPlan, ReplayStatusType.FAIL_INTERRUPTED);
      return Pair.of(null,
          CommonResponse.badResponse("Got empty execution context, " + replayPlan));
    }

    progressTracer.initTotal(replayPlan);
    compareConfigService.preload(replayPlan);
    return Pair.of(replayPlan, null);
  }

  private boolean isStop(String planId) {
    return redisCacheProvider.get(PlanProduceService.buildStopPlanRedisKey(planId)) != null;
  }

  private List<ReplayCaseBatchInfo> buildBatchInfoList(ReplayPlan replayPlan) {
    Set<String> planItemIds = new HashSet<>();
    List<ReplayCaseBatchInfo> replayCaseBatchInfos = new ArrayList<>();
    for (PlanExecutionContext executionContext : replayPlan.getExecutionContexts()) {
      ReplayCaseBatchInfo replayCaseBatchInfo = new ReplayCaseBatchInfo();
      replayCaseBatchInfo.setCaseIds(new ArrayList<>());
      ReplayCaseBatchInfo replayCaseBatchInfoForWarmUp = new ReplayCaseBatchInfo();

      ContextDependenciesHolder dependencyHolder = (ContextDependenciesHolder) executionContext.getDependencies();
      String contextIdentifier = dependencyHolder.getContextIdentifier();
      if (StringUtils.isNotEmpty(contextIdentifier)) {
        // warmUp case
        ReplayActionCaseItem warmupCase = replayActionCaseItemRepository.getOneOfContext(
            replayPlan.getId(),
            dependencyHolder.getContextIdentifier());
        if (warmupCase != null) {
          replayCaseBatchInfoForWarmUp.setWarmUpId(contextIdentifier);
          replayCaseBatchInfoForWarmUp.setCaseIds(Collections.singletonList(warmupCase.getId()));
          replayCaseBatchInfos.add(replayCaseBatchInfoForWarmUp);
        }
      }

      // other cases
      List<ReplayActionCaseItem> caseItems = Collections.emptyList();
      while (true) {
        // checkpoint: before sending page of cases
        ReplayActionCaseItem lastItem =
            CollectionUtils.isNotEmpty(caseItems) ? caseItems.get(caseItems.size() - 1) : null;
        caseItems = replayActionCaseItemRepository.waitingSendList(replayPlan.getId(),
            CommonConstant.MAX_PAGE_SIZE,
            executionContext.getContextCaseQuery(),
            Optional.ofNullable(lastItem).map(ReplayActionCaseItem::getRecordTime).orElse(null));

        if (CollectionUtils.isEmpty(caseItems)) {
          break;
        }
        List<String> caseIdList = replayCaseBatchInfo.getCaseIds();
        caseItems.forEach(replayActionCaseItem -> {
          planItemIds.add(replayActionCaseItem.getPlanItemId());
          caseIdList.add(replayActionCaseItem.getId());
        });
      }
      if (StringUtils.isEmpty(contextIdentifier)) {
        replayCaseBatchInfos.add(0, replayCaseBatchInfo);
      } else {
        replayCaseBatchInfos.add(replayCaseBatchInfo);
      }
    }
    cacheReplayActionItem(replayPlan.getReplayActionItemList(), planItemIds);
    return replayCaseBatchInfos;
  }
}

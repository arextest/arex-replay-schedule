package com.arextest.schedule.service;

import com.arextest.common.cache.CacheProvider;
import com.arextest.common.utils.CompressionUtils;
import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.replay.QueryMockCacheResponseType;
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
import com.arextest.schedule.model.plan.ReplayCaseBatchInfo;
import com.arextest.schedule.plan.PlanContext;
import com.arextest.schedule.plan.PlanContextCreator;
import com.arextest.schedule.plan.builder.BuildPlanValidateResult;
import com.arextest.schedule.plan.builder.ReplayPlanBuilder;
import com.arextest.schedule.planexecution.PlanExecutionContextProvider;
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
public class LocalReplayService {

  private static final String REPLAY_ACTION_ITEM_KEY_FORMAT = "replay_action_item_%s";

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

  public CommonResponse queryReplayCaseId(BuildReplayPlanRequest request) {
    final QueryReplayCaseIdResponse response = new QueryReplayCaseIdResponse();
    final List<ReplayCaseBatchInfo> replayCaseBatchInfos = new ArrayList<>();
    response.setReplayCaseBatchInfos(replayCaseBatchInfos);

    Pair<ReplayPlan, CommonResponse> pair = buildReplayPlan(request);
    if (pair.getLeft() == null) {
      return pair.getRight();
    }
    ReplayPlan replayPlan = pair.getLeft();
    Set<String> planItemIds = new HashSet<>();
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
            Optional.ofNullable(lastItem).map(ReplayActionCaseItem::getId).orElse(null));

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
        replayCaseBatchInfos.set(0, replayCaseBatchInfo);
      } else {
        replayCaseBatchInfos.add(replayCaseBatchInfo);
      }
    }
    response.setPlanId(replayPlan.getId());
    cacheReplayActionItem(replayPlan.getReplayActionItemList(), planItemIds);
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
    QueryMockCacheResponseType queryMockCacheResponseType = mockCachePreLoader.fillMockSource(
        request.getRecordId(), request.getReplayPlanType());
    return queryMockCacheResponseType != null;
  }


  public boolean postSend(PostSendRequest request) {
    CompletableFuture.runAsync(() -> postSend0(request), postSendExecutorService);
    return true;
  }

  private void postSend0(PostSendRequest request) {
    ReplayPlan replayPlan = replayPlanRepository.query(request.getPlanId());
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

  private byte[] buildReplayActionItemRedisKey(String planItemId) {
    return (String.format(REPLAY_ACTION_ITEM_KEY_FORMAT, planItemId)).getBytes(
        StandardCharsets.UTF_8);
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
    int planCaseCount = planBuilder.buildReplayCaseCount(replayActionItemList);
    if (planCaseCount == 0) {
      return Pair.of(null,
          CommonResponse.badResponse("loaded empty case,try change time range submit again ",
              new BuildReplayPlanResponse(BuildReplayFailReasonEnum.NO_CASE_IN_RANGE)));
    }
    replayPlan.setCaseTotalCount(planCaseCount);
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

    planConsumePrepareService.prepareRunData(replayPlan);
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
}

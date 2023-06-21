package com.arextest.schedule.service;

import com.arextest.common.cache.CacheProvider;
import com.arextest.schedule.bizlog.BizLogger;
import com.arextest.schedule.dao.mongodb.ReplayPlanActionRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanRepository;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.plan.PlanContext;
import com.arextest.schedule.plan.PlanContextCreator;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.model.CommonResponse;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.deploy.DeploymentVersion;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.model.plan.BuildReplayPlanRequest;
import com.arextest.schedule.plan.builder.BuildPlanValidateResult;
import com.arextest.schedule.plan.builder.ReplayPlanBuilder;
import com.arextest.schedule.utils.ReplayParentBinder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.arextest.schedule.common.CommonConstant.*;

/**
 * Created by wang_yc on 2021/9/15
 */
@Service
@Slf4j
public class PlanProduceService {
    @Resource
    private List<ReplayPlanBuilder> replayPlanBuilderList;
    @Resource
    private PlanContextCreator planContextCreator;
    @Resource
    private ReplayPlanRepository replayPlanRepository;
    @Resource
    private ReplayPlanActionRepository replayPlanActionRepository;
    @Resource
    private PlanConsumeService planConsumeService;
    @Resource
    private ProgressEvent progressEvent;
    @Resource
    private ConfigurationService configurationService;
    @Resource
    private CacheProvider redisCacheProvider;

    public CommonResponse createPlan(BuildReplayPlanRequest request) {
        progressEvent.onBeforePlanCreate(request);

        long planCreateMillis = System.currentTimeMillis();
        String appId = request.getAppId();
        if (isCreating(appId, request.getTargetEnv())) {
            progressEvent.onReplayPlanCreateException(request);
            return CommonResponse.badResponse("This appid is creating plan");
        }
        ReplayPlanBuilder planBuilder = select(request);
        if (planBuilder == null) {
            progressEvent.onReplayPlanCreateException(request);
            return CommonResponse.badResponse("appId:" + appId + " unsupported replay planType : " + request.getReplayPlanType());
        }
        PlanContext planContext = planContextCreator.createByAppId(appId);
        BuildPlanValidateResult result = planBuilder.validate(request, planContext);
        if (result.failure()) {
            progressEvent.onReplayPlanCreateException(request);
            return CommonResponse.badResponse("appId:" + appId + " error: " + result.getRemark());
        }
        List<ReplayActionItem> replayActionItemList = planBuilder.buildReplayActionList(request, planContext);
        if (CollectionUtils.isEmpty(replayActionItemList)) {
            progressEvent.onReplayPlanCreateException(request);
            return CommonResponse.badResponse("appId:" + appId + " error: empty replay actions");
        }
        ReplayPlan replayPlan = build(request, planContext);
        replayPlan.setPlanCreateMillis(planCreateMillis);
        replayPlan.setReplayActionItemList(replayActionItemList);
        ReplayParentBinder.setupReplayActionParent(replayActionItemList, replayPlan);
        int planCaseCount = planBuilder.buildReplayCaseCount(replayActionItemList);
        if (planCaseCount == 0) {
            return CommonResponse.badResponse("loaded empty case,try change time range submit again ");
        }
        replayPlan.setCaseTotalCount(planCaseCount);
        // todo: add trans
        if (!replayPlanRepository.save(replayPlan)) {
            progressEvent.onReplayPlanCreateException(request);
            return CommonResponse.badResponse("save replan plan error, " + replayPlan.toString());
        }
        MDCTracer.addPlanId(replayPlan.getId());
        if (!replayPlanActionRepository.save(replayActionItemList)) {
            progressEvent.onReplayPlanCreateException(request);
            return CommonResponse.badResponse("save replay action error, " + replayPlan.toString());
        }

        BizLogger.recordPlanStart(replayPlan);
        progressEvent.onReplayPlanCreated(replayPlan);
        planConsumeService.runAsyncConsume(replayPlan);
        return CommonResponse.successResponse("create plan success！" + result.getRemark(), replayPlan.getId());
    }

    private ReplayPlan build(BuildReplayPlanRequest request, PlanContext planContext) {
        String appId = request.getAppId();
        ReplayPlan replayPlan = new ReplayPlan();
        replayPlan.setAppId(appId);
        replayPlan.setPlanName(request.getPlanName());
        DeploymentVersion deploymentVersion = planContext.getTargetVersion();
        if (deploymentVersion != null) {
            replayPlan.setTargetImageId(deploymentVersion.getImageId());
            replayPlan.setTargetImageName(deploymentVersion.getImage().getName());
        }
        List<ServiceInstance> serviceInstances = planContext.targetActiveInstance();
        replayPlan.setTargetHost(getIpAddress(serviceInstances));
        replayPlan.setTargetEnv(request.getTargetEnv());
        serviceInstances = planContext.sourceActiveInstance();
        if (CollectionUtils.isEmpty(serviceInstances)) {
            replayPlan.setSourceEnv(StringUtils.EMPTY);
            replayPlan.setSourceHost(StringUtils.EMPTY);
        } else {
            replayPlan.setSourceEnv(request.getSourceEnv());
            replayPlan.setSourceHost(getIpAddress(serviceInstances));
        }
        replayPlan.setPlanCreateTime(new Date());
        replayPlan.setOperator(request.getOperator());
        replayPlan.setCaseSourceFrom(request.getCaseSourceFrom());
        replayPlan.setCaseSourceTo(request.getCaseSourceTo());
        replayPlan.setCaseSourceType(request.getCaseSourceType());
        replayPlan.setReplayPlanType(request.getReplayPlanType());
        ConfigurationService.Application replayApp = configurationService.application(appId);
        if (replayApp != null) {
            replayPlan.setArexCordVersion(replayApp.getAgentVersion());
            replayPlan.setArexExtVersion(replayApp.getAgentExtVersion());
            replayPlan.setCaseRecordVersion(replayApp.getAgentExtVersion());
            replayPlan.setAppName(replayApp.getAppName());
        }
        ConfigurationService.ScheduleConfiguration schedule = configurationService.schedule(appId);
        if (schedule != null) {
            replayPlan.setReplaySendMaxQps(schedule.getSendMaxQps());
        }
        if (request.getCaseCountLimit() <= 0) {
            replayPlan.setCaseCountLimit(OPERATION_MAX_CASE_COUNT);
        } else {
            replayPlan.setCaseCountLimit(request.getCaseCountLimit());
        }

        replayPlan.setMinInstanceCount(planContext.determineMinInstanceCount());
        return replayPlan;
    }

    private String getIpAddress(List<ServiceInstance> serviceInstances) {
        return serviceInstances.stream().map(ServiceInstance::getIp).collect(Collectors.joining(","));
    }

    private ReplayPlanBuilder select(BuildReplayPlanRequest request) {
        for (ReplayPlanBuilder replayPlanBuilder : replayPlanBuilderList) {
            if (replayPlanBuilder.isSupported(request)) {
                return replayPlanBuilder;
            }
        }
        return null;
    }

    public Boolean isCreating(String appId, String targetEnv) {
        try {
            byte[] key = String.format("schedule_creating_%s_%s", appId, targetEnv).getBytes(StandardCharsets.UTF_8);
            byte[] value = appId.getBytes(StandardCharsets.UTF_8);
            Boolean result = redisCacheProvider.putIfAbsent(key, CREATE_PLAN_REDIS_EXPIRE,value);
            return !result;
        } catch (Exception e) {
            LOGGER.error("isCreating error : {}", e.getMessage(), e);
            return true;
        }
    }

    public void removeCreating(String appId, String targetEnv) {
        try {
            byte[] key = String.format("schedule_creating_%s_%s", appId, targetEnv).getBytes(StandardCharsets.UTF_8);
            redisCacheProvider.remove(key);
        } catch (Exception e) {
            LOGGER.error("removeCreating error : {}", e.getMessage(), e);
        }
    }

    public void stopPlan(String planId) {
        try {
            // set key for other instance to stop internal execution
            redisCacheProvider.putIfAbsent(buildStopPlanRedisKey(planId),
                    STOP_PLAN_REDIS_EXPIRE, planId.getBytes(StandardCharsets.UTF_8));

            // set the canceled status immediately to give quick response to user
            progressEvent.onReplayPlanTerminate(planId);
        } catch (Exception e) {
            LOGGER.error("stopPlan error, planId: {}, message: {}", planId, e.getMessage());
        }
    }

    public static byte[] buildStopPlanRedisKey(String planId) {
        return (STOP_PLAN_REDIS_KEY + planId).getBytes(StandardCharsets.UTF_8);
    }
}

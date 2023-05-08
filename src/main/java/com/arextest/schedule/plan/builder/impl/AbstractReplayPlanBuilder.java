package com.arextest.schedule.plan.builder.impl;

import com.arextest.schedule.model.AppServiceDescriptor;
import com.arextest.schedule.model.CaseSourceEnvType;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.deploy.DeploymentVersion;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.model.plan.BuildReplayPlanRequest;
import com.arextest.schedule.plan.PlanContext;
import com.arextest.schedule.plan.builder.BuildPlanValidateResult;
import com.arextest.schedule.plan.builder.ReplayPlanBuilder;
import com.arextest.schedule.service.DefaultDeployedEnvironmentService;
import com.arextest.schedule.service.ReplayActionItemPreprocessService;
import com.arextest.schedule.service.ReplayCaseRemoteLoadService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author jmo
 * @since 2021/9/22
 */
abstract class AbstractReplayPlanBuilder implements ReplayPlanBuilder {
    static final int APP_SUSPENDED_STATUS = -1;
    private final static String DEFAULT_PRO_SOURCE_ENV = "pro";
    @Resource
    private DefaultDeployedEnvironmentService deployedEnvironmentService;
    @Resource
    private ReplayCaseRemoteLoadService replayCaseRemoteLoadService;
    @Resource
    private ReplayActionItemPreprocessService replayActionItemPreprocessService;


    @Override
    public BuildPlanValidateResult validate(BuildReplayPlanRequest request, PlanContext planContext) {
        List<AppServiceDescriptor> serviceDescriptors = planContext.getAppServiceDescriptorList();
        if (CollectionUtils.isEmpty(serviceDescriptors)) {
            return BuildPlanValidateResult.create(BuildPlanValidateResult.APP_ID_NOT_FOUND_SERVICE, "the appId not found any services");
        }
        if (CaseSourceEnvType.toCaseSourceType(request.getCaseSourceType()) == null) {
            return BuildPlanValidateResult.create(BuildPlanValidateResult.UNSUPPORTED_CASE_SOURCE_TYPE, "unsupported case source type");
        }
        if (unsupportedCaseTimeRange(request)) {
            return BuildPlanValidateResult.create(BuildPlanValidateResult.REQUESTED_CASE_TIME_RANGE_UNSUPPORTED,
                    "requested case time range unsupported");
        }
        String appId = request.getAppId();
        String env = request.getTargetEnv();
        if (unableLoadActiveInstance(serviceDescriptors, env, AppServiceDescriptor::setTargetActiveInstanceList)) {
            return BuildPlanValidateResult.create(BuildPlanValidateResult.REQUESTED_TARGET_ENV_UNAVAILABLE, "requested target env unable load" +
                    " active instance");
        }
        DeploymentVersion deploymentVersion = deployedEnvironmentService.getVersionEvent(appId, env);
        planContext.setTargetVersion(deploymentVersion);
        env = request.getSourceEnv();
        if (StringUtils.isNotBlank(env) && !StringUtils.equals(DEFAULT_PRO_SOURCE_ENV, env)) {
            if (unableLoadActiveInstance(serviceDescriptors, env, AppServiceDescriptor::setSourceActiveInstanceList)) {
                return BuildPlanValidateResult.create(BuildPlanValidateResult.REQUESTED_SOURCE_ENV_UNAVAILABLE,
                        "requested source env unable load active instance");
            }
            deploymentVersion = deployedEnvironmentService.getVersionEvent(appId, env);
            planContext.setSourceVersion(deploymentVersion);
        }
        return BuildPlanValidateResult.createSuccess();
    }

    private boolean unableLoadActiveInstance(List<AppServiceDescriptor> descriptorList, String env,
                                             BiConsumer<AppServiceDescriptor, List<ServiceInstance>> bindTo) {
        List<ServiceInstance> instanceList;
        boolean hasInstance = false;
        AppServiceDescriptor appServiceDescriptor;
        for (int i = 0; i < descriptorList.size(); i++) {
            appServiceDescriptor = descriptorList.get(i);
            instanceList = deployedEnvironmentService.getActiveInstanceListEvent(appServiceDescriptor, env);
            if (CollectionUtils.isNotEmpty(instanceList)) {
                hasInstance = true;
            }
            bindTo.accept(appServiceDescriptor, instanceList);
        }
        return !hasInstance;
    }

    @Override
    public List<ReplayActionItem> buildReplayActionList(BuildReplayPlanRequest request, PlanContext planContext) {
        List<ReplayActionItem> replayActionItemList = getReplayActionList(request, planContext);
        replayActionItemPreprocessService.addExclusionOperation(replayActionItemList, planContext.getAppId());
        return replayActionItemList;
    }

    @Override
    public int buildReplayCaseCount(List<ReplayActionItem> actionItemList) {
        int sum = 0;
        int actionCount;
        for (int i = 0; i < actionItemList.size(); i++) {
            ReplayActionItem actionItem = actionItemList.get(i);
            actionCount = queryCaseCount(actionItem);
            actionItem.setReplayCaseCount(actionCount);
            sum += actionCount;
        }
        return sum;
    }

    abstract List<ReplayActionItem> getReplayActionList(BuildReplayPlanRequest request, PlanContext planContext);

    int queryCaseCount(ReplayActionItem actionItem) {
        return replayCaseRemoteLoadService.queryCaseCount(actionItem);
    }

    boolean unsupportedCaseTimeRange(BuildReplayPlanRequest request) {
        Date fromDate = request.getCaseSourceFrom();
        Date toDate = request.getCaseSourceTo();
        if (fromDate != null && toDate != null) {
            return fromDate.after(toDate);
        }
        return false;
    }

}
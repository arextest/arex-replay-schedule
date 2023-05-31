package com.arextest.schedule.plan.builder.impl;

import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.model.AppServiceDescriptor;
import com.arextest.schedule.model.CaseSourceEnvType;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.deploy.DeploymentVersion;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.model.plan.BuildReplayPlanRequest;
import com.arextest.schedule.model.plan.OperationCaseInfo;
import com.arextest.schedule.plan.PlanContext;
import com.arextest.schedule.plan.builder.BuildPlanValidateResult;
import com.arextest.schedule.plan.builder.ReplayPlanBuilder;
import com.arextest.schedule.service.DeployedEnvironmentService;
import com.arextest.schedule.service.ReplayActionItemPreprocessService;
import com.arextest.schedule.service.ReplayCaseRemoteLoadService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author jmo
 * @since 2021/9/22
 */
abstract class AbstractReplayPlanBuilder implements ReplayPlanBuilder {
    static final int APP_SUSPENDED_STATUS = -1;
    private final static String DEFAULT_PRO_SOURCE_ENV = "pro";
    @Resource
    private DeployedEnvironmentService deployedEnvironmentService;
    @Resource
    private ReplayCaseRemoteLoadService replayCaseRemoteLoadService;
    @Resource
    private ReplayActionItemPreprocessService replayActionItemPreprocessService;


    @Override
    public void filterAppServiceDescriptors(BuildReplayPlanRequest request, PlanContext planContext) {
        if (request == null || CollectionUtils.isEmpty(request.getOperationCaseInfoList()) || planContext == null) {
            return;
        }
        planContext.setAppServiceDescriptorList(planContext.filterAppServiceDescriptors(request.getOperationCaseInfoList().stream()
                .map(OperationCaseInfo::getOperationId)
                .collect(Collectors.toList())));
    }

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
        DeploymentVersion deploymentVersion = deployedEnvironmentService.getVersion(appId, env);
        planContext.setTargetVersion(deploymentVersion);
        env = request.getSourceEnv();
        if (StringUtils.isNotBlank(env) && !StringUtils.equals(DEFAULT_PRO_SOURCE_ENV, env)) {
            if (unableLoadActiveInstance(serviceDescriptors, env, AppServiceDescriptor::setSourceActiveInstanceList)) {
                return BuildPlanValidateResult.create(BuildPlanValidateResult.REQUESTED_SOURCE_ENV_UNAVAILABLE,
                        "requested source env unable load active instance");
            }
            deploymentVersion = deployedEnvironmentService.getVersion(appId, env);
            planContext.setSourceVersion(deploymentVersion);
        }
        return BuildPlanValidateResult.createSuccess();
    }

    private boolean unableLoadActiveInstance(List<AppServiceDescriptor> descriptorList, String env,
                                             BiConsumer<AppServiceDescriptor, List<ServiceInstance>> bindTo) {
        boolean hasInstance = false;
        for (Iterator<AppServiceDescriptor> iterator = descriptorList.iterator(); iterator.hasNext(); ) {
            AppServiceDescriptor appServiceDescriptor = iterator.next();
            List<ServiceInstance> instanceList = deployedEnvironmentService.getActiveInstanceList(appServiceDescriptor, env);
            if (CollectionUtils.isNotEmpty(instanceList)) {
                hasInstance = true;
                bindTo.accept(appServiceDescriptor, instanceList);
            } else {
                iterator.remove();
            }
        }
        return !hasInstance;
    }

    @Override
    public List<ReplayActionItem> buildReplayActionList(BuildReplayPlanRequest request, PlanContext planContext) {
        List<ReplayActionItem> replayActionItemList = getReplayActionList(request, planContext);
        replayActionItemPreprocessService.filterActionItemAndAddExclusionOperation(replayActionItemList, planContext.getAppId());
        return replayActionItemList;
    }

    @Override
    public int buildReplayCaseCount(List<ReplayActionItem> actionItemList) {
        int sum = 0;
        int actionCount;
        int maxCount = 0;
        for (int i = 0; i < actionItemList.size(); i++) {
            ReplayActionItem actionItem = actionItemList.get(i);
            actionCount = queryCaseCount(actionItem);
            actionItem.setReplayCaseCount(actionCount);
            if (actionCount > maxCount) {
                maxCount = actionCount;
            }
            sum += actionCount;
        }
        if (maxCount > 0) {
            actionItemList.get(0).getParent().setCaseCountLimit(maxCount);
        } else {
            actionItemList.get(0).getParent().setCaseCountLimit(CommonConstant.MAX_PAGE_SIZE);
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
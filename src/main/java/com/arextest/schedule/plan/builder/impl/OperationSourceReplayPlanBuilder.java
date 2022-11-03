package com.arextest.schedule.plan.builder.impl;

import com.arextest.schedule.model.AppServiceOperationDescriptor;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.plan.BuildReplayPlanRequest;
import com.arextest.schedule.model.plan.BuildReplayPlanType;
import com.arextest.schedule.model.plan.OperationCaseInfo;
import com.arextest.schedule.plan.PlanContext;
import com.arextest.schedule.plan.builder.BuildPlanValidateResult;
import com.arextest.schedule.service.ReplayActionItemPreprocessService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jmo
 * @since 2021/9/18
 */
@Component
final class OperationSourceReplayPlanBuilder extends AbstractReplayPlanBuilder {

    @Resource
    private ReplayActionItemPreprocessService replayActionItemPreprocessService;

    @Override
    public boolean isSupported(BuildReplayPlanRequest request) {
        return request.getReplayPlanType() == BuildReplayPlanType.BY_OPERATION_OF_APP_ID.getValue();
    }

    @Override
    public BuildPlanValidateResult validate(BuildReplayPlanRequest request, PlanContext planContext) {
        if (CollectionUtils.isEmpty(request.getOperationCaseInfoList())) {
            return BuildPlanValidateResult.create(BuildPlanValidateResult.REQUESTED_EMPTY_OPERATION, "REQUESTED_EMPTY_OPERATION");
        }
        for (OperationCaseInfo requestedOperation : request.getOperationCaseInfoList()) {
            if (planContext.findAppServiceOperationDescriptor(requestedOperation.getOperationId()) == null) {
                return BuildPlanValidateResult.create(BuildPlanValidateResult.REQUESTED_OPERATION_NOT_FOUND, "REQUESTED_OPERATION_NOT_FOUND");
            }
        }
        return super.validate(request, planContext);
    }

    @Override
    List<ReplayActionItem> getReplayActionList(BuildReplayPlanRequest request, PlanContext planContext) {
        List<ReplayActionItem> replayActionItemList = new ArrayList<>();
        AppServiceOperationDescriptor operationDescriptor;
        for (OperationCaseInfo operationCaseInfo : request.getOperationCaseInfoList()) {
            operationDescriptor = planContext.findAppServiceOperationDescriptor(operationCaseInfo.getOperationId());
            if (operationDescriptor == null) {
                continue;
            }
            ReplayActionItem replayActionItem = planContext.toReplayAction(operationDescriptor);
            replayActionItemList.add(replayActionItem);
        }
        replayActionItemPreprocessService.addExclusionOperation(replayActionItemList, planContext.getAppId());
        return replayActionItemList;
    }
}
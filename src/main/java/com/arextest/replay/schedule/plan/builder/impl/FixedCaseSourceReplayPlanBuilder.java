package com.arextest.replay.schedule.plan.builder.impl;

import com.arextest.replay.schedule.model.AppServiceOperationDescriptor;
import com.arextest.replay.schedule.model.ReplayActionCaseItem;
import com.arextest.replay.schedule.model.ReplayActionItem;
import com.arextest.replay.schedule.model.plan.BuildReplayPlanRequest;
import com.arextest.replay.schedule.model.plan.BuildReplayPlanType;
import com.arextest.replay.schedule.model.plan.OperationCaseInfo;
import com.arextest.replay.schedule.plan.PlanContext;
import com.arextest.replay.schedule.plan.builder.BuildPlanValidateResult;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jmo
 * @since 2021/9/18
 */
@Component
final class FixedCaseSourceReplayPlanBuilder extends AbstractReplayPlanBuilder {

    @Override
    public boolean isSupported(BuildReplayPlanRequest request) {
        return request.getReplayPlanType() == BuildReplayPlanType.BY_FIXED_CASE.getValue();
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
            if (CollectionUtils.isEmpty(requestedOperation.getReplayIdList())) {
                return BuildPlanValidateResult.create(BuildPlanValidateResult.REQUESTED_OPERATION_NOT_FOUND_ANY_CASE,
                        "REQUESTED_OPERATION_NOT_FOUND_ANY_CASE");
            }
        }
        return super.validate(request, planContext);
    }

    @Override
    boolean unsupportedCaseTimeRange(BuildReplayPlanRequest request) {
        return false;
    }

    @Override
    public List<ReplayActionItem> buildReplayActionList(BuildReplayPlanRequest request, PlanContext planContext) {
        final List<ReplayActionItem> replayActionItemList = new ArrayList<>();
        AppServiceOperationDescriptor operationDescriptor;
        for (OperationCaseInfo operationCaseInfo : request.getOperationCaseInfoList()) {
            operationDescriptor = planContext.findAppServiceOperationDescriptor(operationCaseInfo.getOperationId());
            if (operationDescriptor == null) {
                continue;
            }
            ReplayActionItem replayActionItem = planContext.toReplayAction(operationDescriptor);
            List<String> replayIdList = operationCaseInfo.getReplayIdList();
            ReplayActionCaseItem caseItem;
            final List<ReplayActionCaseItem> caseItemList = new ArrayList<>(replayIdList.size());
            for (String replayId : replayIdList) {
                caseItem = new ReplayActionCaseItem();
                caseItem.setRecordId(replayId);
                caseItem.setParent(replayActionItem);
                caseItemList.add(caseItem);
            }
            replayActionItem.setCaseItemList(caseItemList);
            replayActionItem.setReplayCaseCount(caseItemList.size());
            replayActionItemList.add(replayActionItem);
        }
        return replayActionItemList;
    }

    @Override
    int queryCaseCount(ReplayActionItem actionItem) {
        return actionItem.getCaseItemList().size();
    }
}

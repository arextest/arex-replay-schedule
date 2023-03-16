package com.arextest.schedule.plan.builder.impl;

import com.arextest.schedule.model.AppServiceOperationDescriptor;
import com.arextest.schedule.model.ReplayActionCaseItem;
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
final class FixedCaseSourceReplayPlanBuilder extends AbstractReplayPlanBuilder {

    @Resource
    private ReplayActionItemPreprocessService replayActionItemPreprocessService;

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
    List<ReplayActionItem> getReplayActionList(BuildReplayPlanRequest request, PlanContext planContext) {
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
        replayActionItemPreprocessService.addExclusionOperation(replayActionItemList, planContext.getAppId());
        return replayActionItemList;
    }

    @Override
    int queryCaseCount(ReplayActionItem actionItem, Integer caseCountLimit) {
        return actionItem.getCaseItemList().size();
    }
}
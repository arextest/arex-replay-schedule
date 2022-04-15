package io.arex.replay.schedule.plan.builder.impl;

import io.arex.replay.schedule.model.AppServiceDescriptor;
import io.arex.replay.schedule.model.AppServiceOperationDescriptor;
import io.arex.replay.schedule.model.ReplayActionItem;
import io.arex.replay.schedule.model.plan.BuildReplayPlanRequest;
import io.arex.replay.schedule.model.plan.BuildReplayPlanType;
import io.arex.replay.schedule.plan.PlanContext;
import io.arex.replay.schedule.plan.builder.BuildPlanValidateResult;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jmo
 * @since 2021/9/18
 */
@Component
final class AppIdSourceReplayPlanBuilder extends AbstractReplayPlanBuilder {

    @Override
    public BuildPlanValidateResult validate(BuildReplayPlanRequest request, PlanContext planContext) {
        return super.validate(request, planContext);
    }

    @Override
    public List<ReplayActionItem> buildReplayActionList(BuildReplayPlanRequest request, PlanContext planContext) {
        List<AppServiceOperationDescriptor> operationDescriptorList;
        List<ReplayActionItem> replayActionItemList = new ArrayList<>();
        for (AppServiceDescriptor appServiceDescriptor : planContext.getAppServiceDescriptorList()) {
            operationDescriptorList = appServiceDescriptor.getOperationList();
            if (CollectionUtils.isEmpty(operationDescriptorList)) {
                continue;
            }
            for (AppServiceOperationDescriptor operationDescriptor : operationDescriptorList) {
                // -1是挂起 不进行回放的接口
                if (operationDescriptor.getStatus() == AbstractReplayPlanBuilder.APP_SUSPENDED_STATUS
                        || operationDescriptor.getOperationName().equalsIgnoreCase("checkHealth")) {
                    continue;
                }
                ReplayActionItem replayActionItem = planContext.toReplayAction(operationDescriptor);
                replayActionItemList.add(replayActionItem);
            }
        }
        return replayActionItemList;
    }

    @Override
    public boolean isSupported(BuildReplayPlanRequest request) {
        return request.getReplayPlanType() == BuildReplayPlanType.BY_APP_ID.getValue();
    }
}

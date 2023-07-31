package com.arextest.schedule.planexecution.impl;

import com.arextest.schedule.dao.mongodb.ReplayPlanRepository;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.plan.PlanStageEnum;
import com.arextest.schedule.model.plan.ReplayPlanStageInfo;
import com.arextest.schedule.model.plan.StageStatusEnum;
import com.arextest.schedule.planexecution.PlanMonitorHandler;
import com.arextest.schedule.planexecution.impl.PlanExecutionMonitorImpl;
import javax.annotation.Resource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author wildeslam.
 * @create 2023/7/31 15:47
 */
@Component
@Order(2)
public class StageUpdateHandler implements PlanMonitorHandler {
    @Resource
    private ReplayPlanRepository replayPlanRepository;

    @Override
    public void handle(ReplayPlan plan) {
        if (plan.getPlanStatus() != null && plan.getPlanStatus().isCanceled()) {
            addCancelStage(plan);
        }
        if (System.currentTimeMillis() - plan.getLastUpdateTime() < PlanExecutionMonitorImpl.SECOND_TO_REFRESH) {
            replayPlanRepository.updateStage(plan);
        }
    }

    @Override
    public void end(ReplayPlan plan) {
        replayPlanRepository.updateStage(plan);
    }

    private void addCancelStage(ReplayPlan replayPlan) {
        int index = 0;
        for (; index < replayPlan.getReplayPlanStageList().size(); index++) {
            if (replayPlan.getReplayPlanStageList().get(index).getStageStatus() == StageStatusEnum.PENDING.getCode()) {
                break;
            }
        }
        ReplayPlanStageInfo cancelStage = new ReplayPlanStageInfo();
        cancelStage.setStageStatus(StageStatusEnum.SUCCEEDED.getCode());
        cancelStage.setStageType(PlanStageEnum.CANCEL.getCode());
        cancelStage.setStageName(PlanStageEnum.CANCEL.name());
        replayPlan.getReplayPlanStageList().add(Math.max(0, index - 1), cancelStage);
        replayPlanRepository.updateStage(replayPlan);
    }
}

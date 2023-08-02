package com.arextest.schedule.planexecution.impl;

import com.arextest.schedule.dao.mongodb.ReplayPlanRepository;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.plan.PlanStageEnum;
import com.arextest.schedule.model.plan.ReplayPlanStageInfo;
import com.arextest.schedule.model.plan.StageStatusEnum;
import com.arextest.schedule.planexecution.PlanMonitorHandler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

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
        // expand SECOND_TO_REFRESH to avoid the edge case.
        if (System.currentTimeMillis() - plan.getLastUpdateTime() < PlanExecutionMonitorImpl.SECOND_TO_REFRESH * 2 * 1000) {
            replayPlanRepository.updateStage(plan);
        }
    }

    @Override
    public void end(ReplayPlan plan) {
        if (plan.getPlanStatus() != null && plan.getPlanStatus().isCanceled()) {
            addCancelStage(plan);
        }
        ReplayPlanStageInfo runStage = plan.getReplayPlanStageList().stream()
            .filter(stage -> stage.getStageStatus() == PlanStageEnum.RUN.getCode())
            .findFirst()
            .orElse(null);
        if (runStage != null && runStage.getStageStatus() == StageStatusEnum.ONGOING.getCode()) {
            runStage.setStageStatus(StageStatusEnum.FAILED.getCode());
        }
        replayPlanRepository.updateStage(plan);
    }

    private void addCancelStage(ReplayPlan replayPlan) {
        for (ReplayPlanStageInfo stage : replayPlan.getReplayPlanStageList()) {
            if (stage.getStageType() == PlanStageEnum.CANCEL.getCode()) {
                return;
            }
        }
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
        replayPlan.getReplayPlanStageList().add(index, cancelStage);
        replayPlanRepository.updateStage(replayPlan);
    }
}

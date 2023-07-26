package com.arextest.schedule.planexecution;

import com.arextest.schedule.dao.mongodb.ReplayPlanRepository;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.plan.PlanStageEnum;
import com.arextest.schedule.model.plan.ReplayPlanStageInfo;
import com.arextest.schedule.model.plan.StageStatusEnum;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author wildeslam.
 * @create 2023/7/25 20:03
 */
@Slf4j
@Component
public class PlanStageMonitorImpl implements PlanExecutionMonitor {
    @Resource
    private ScheduledExecutorService updatePlanStageScheduler;
    @Resource
    private ReplayPlanRepository replayPlanRepository;
    @Resource
    private RedisCancelMonitor cancelMonitor;

    @Value("${arex.schedule.monitor.secondToRefresh}")
    public int SECOND_TO_REFRESH;

    private static final ConcurrentHashMap<String, List<ReplayPlanStageInfo>> replayPlanStageListMap =
        new ConcurrentHashMap<>();

    /**
     * Monitor task for each Replay plan
     */
    private class MonitorTask implements Runnable {
        ReplayPlan replayPlan;

        MonitorTask(ReplayPlan replayPlan) {
            this.replayPlan = replayPlan;
        }

        @Override
        public void run() {
            try {
                monitorOne(replayPlan);
            } catch (Throwable t) {
                LOGGER.error("Error update plan stage", t);
            } finally {
                MDCTracer.clear();
            }
        }
    }


    @Override
    public void register(ReplayPlan plan) {
        MonitorTask task = new MonitorTask(plan);
        ScheduledFuture<?> updateFuture = updatePlanStageScheduler
            .scheduleAtFixedRate(task, 0, SECOND_TO_REFRESH, TimeUnit.SECONDS);
        plan.setStageFuture(updateFuture);
    }

    @Override
    public void deregister(ReplayPlan plan) {
        plan.getStageFuture().cancel(false);
        replayPlanRepository.updateStage(plan);
    }

    @Override
    public void monitorOne(ReplayPlan plan) {
        if (plan != null) {
            LOGGER.info("update plan stage {}", plan.getId());
            if ((plan.getPlanStatus() != null && plan.getPlanStatus().isCanceled())
                || cancelMonitor.isPlanCanceled(plan)) {
                plan.getStageFuture().cancel(false);
                addCancelStage(plan);
            }
            if (replayPlanStageListMap.get(plan.getId()) != plan.getReplayPlanStageList()) {
                replayPlanStageListMap.put(plan.getId(), plan.getReplayPlanStageList());
                replayPlanRepository.updateStage(plan);
            }

        }
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
        replayPlan.getReplayPlanStageList().add(index - 1, cancelStage);
        replayPlanRepository.updateStage(replayPlan);
    }
}

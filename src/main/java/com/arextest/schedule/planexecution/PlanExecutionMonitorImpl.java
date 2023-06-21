package com.arextest.schedule.planexecution;

import com.arextest.common.cache.CacheProvider;
import com.arextest.schedule.common.SendSemaphoreLimiter;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.service.PlanProduceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Qzmo on 2023/6/16
 */
@Component
@Slf4j
public class PlanExecutionMonitorImpl implements PlanExecutionMonitor {
    private final Map<String, ReplayPlan> tasks = new ConcurrentHashMap<>();
    @Resource
    private CacheProvider redisCacheProvider;
    @Resource
    private ProgressTracer progressTracer;

    private final RedisCancelMonitor cancelMonitor = new RedisCancelMonitor();
    private final ProgressMonitor progressMonitor = new ProgressMonitor();
    private final QpsLimiterInterruptMonitor interruptMonitor = new QpsLimiterInterruptMonitor();

    @Value("${arex.schedule.monitor.secondToRefresh}")
    public Integer SECOND_TO_REFRESH;

    @PostConstruct
    public void init() {
        Timer timer = new Timer();
        MonitorTask task = new MonitorTask();
        timer.scheduleAtFixedRate(task, 0, SECOND_TO_REFRESH * 1000);
    }

    private class MonitorTask extends TimerTask {
        @Override
        public void run() {
            try {
                long start = System.currentTimeMillis();
                LOGGER.info("Monitor begins task, monitoring {} tasks", tasks.size());
                monitorAll();
                long end = System.currentTimeMillis();
                LOGGER.info("Monitor begins done, took {} ms", end - start);
            } catch (Throwable t) {
                LOGGER.error("Monitor thread got error", t);
            }
        }
    }

    private void monitorAll() {
        this.tasks.forEach((taskId, task) -> {
            monitorOne(task);
        });
    }

    private void monitorOne(ReplayPlan task) {
        LOGGER.info("Monitoring task {}", task.getId());
        refreshLastUpdateTime(task);
        refreshInterruptStatus(task);
        refreshCancelStatus(task);
    }

    @Override
    public void register(ReplayPlan plan) {
        tasks.put(plan.getId(), plan);
    }

    @Override
    public void deregister(ReplayPlan plan) {
        tasks.remove(plan.getId());
    }

    private void refreshLastUpdateTime(ReplayPlan plan) {
        this.progressMonitor.refreshLastUpdateTime(plan.getId());
    }

    private void refreshCancelStatus(ReplayPlan plan) {
        // to avoid unnecessary redis query
        if (plan.getPlanStatus().isCanceled()) {
            return;
        }
        boolean planCanceled = cancelMonitor.isPlanCanceled(plan);
        if (planCanceled) {
            LOGGER.info("Plan {} cancel status set to true", plan.getId());
            plan.getPlanStatus().setCanceled(true);
        }
    }

    private void refreshInterruptStatus(ReplayPlan plan) {
        plan.getPlanStatus().setInterrupted(interruptMonitor.isPlanNeedToInterrupt(plan));
    }

    private class RedisCancelMonitor {
        private boolean isPlanCanceled(ReplayPlan plan) {
            return isPlanCanceled(plan.getId());
        }
        private boolean isPlanCanceled(String planId) {
            return isCancelled(PlanProduceService.buildStopPlanRedisKey(planId));
        }

        private boolean isCancelled(byte[] redisKey) {
            byte[] bytes = redisCacheProvider.get(redisKey);
            return bytes != null;
        }
    }

    private class ProgressMonitor {
        private void refreshLastUpdateTime(String planId) {
            progressTracer.refreshUpdateTime(planId);
        }
    }

    private class QpsLimiterInterruptMonitor {
        private boolean isPlanNeedToInterrupt(ReplayPlan plan) {
            return Optional.ofNullable(plan.getLimiter())
                    .map(SendSemaphoreLimiter::failBreak)
                    .orElse(false);
        }
    }
}

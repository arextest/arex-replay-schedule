package com.arextest.schedule.planexecution;

import com.arextest.common.cache.CacheProvider;
import com.arextest.schedule.common.SendSemaphoreLimiter;
import com.arextest.schedule.model.ReplayPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.arextest.schedule.common.CommonConstant.STOP_PLAN_REDIS_KEY;

/**
 * Created by Qzmo on 2023/6/16
 */
@Component
@Slf4j
public class PlanExecutionMonitorImpl implements PlanExecutionMonitor {
    private final Map<String, ReplayPlan> tasks = new ConcurrentHashMap<>();
    @Resource
    private CacheProvider redisCacheProvider;

    private final RedisCancelMonitor cancelMonitor = new RedisCancelMonitor();
    private final QpsLimiterInterruptMonitor interruptMonitor = new QpsLimiterInterruptMonitor();

    @Value("${arex.schedule.monitor.secondToRefresh}")
    public Integer SECOND_TO_REFRESH;

    @Override
    public void join(ReplayPlan plan) {
        tasks.put(plan.getId(), plan);
    }

    @Override
    public void exit(ReplayPlan plan) {
        tasks.remove(plan.getId());
    }

    @PostConstruct
    public void init() {
        Thread runner = new Thread(() -> {
            while (true) {
                try {
                    refreshAll();
                    Thread.sleep(SECOND_TO_REFRESH * 1000);
                } catch (Throwable t) {
                    LOGGER.error("Schedule monitor error: ", t);
                }
            }
        });

        runner.setName("Schedule-Monitor");
        runner.setDaemon(true);
        runner.start();
    }

    private void refreshAll() {
        this.tasks.forEach((taskId, task) -> {
            refreshInterruptStatus(task);
            refreshCancelStatus(task);
        });
    }

    private void refreshCancelStatus(ReplayPlan plan) {
        plan.getPlanStatus().setCanceled(cancelMonitor.isPlanCanceled(plan));
    }

    private void refreshInterruptStatus(ReplayPlan plan) {
        plan.getPlanStatus().setInterrupted(interruptMonitor.isPlanNeedToInterrupt(plan));
    }

    private class RedisCancelMonitor {
        private boolean isPlanCanceled(ReplayPlan plan) {
            return isPlanCanceled(plan.getId());
        }
        private boolean isPlanCanceled(String planId) {
            byte[] cancelKey = getCancelKey(planId);
            return isCancelled(cancelKey);
        }

        private boolean isCancelled(byte[] redisKey) {
            byte[] bytes = redisCacheProvider.get(redisKey);
            return bytes != null;
        }

        private byte[] getCancelKey(String planId) {
            return (STOP_PLAN_REDIS_KEY + planId).getBytes(StandardCharsets.UTF_8);
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

package com.arextest.schedule.planexecution;

import com.arextest.common.cache.CacheProvider;
import com.arextest.schedule.dao.mongodb.ReplayBizLogRepository;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.bizlog.BizLog;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.service.PlanProduceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.BlockingQueue;
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
    @Resource
    private ReplayBizLogRepository replayBizLogRepository;

    @Value("${arex.schedule.monitor.secondToRefresh}")
    public Integer SECOND_TO_REFRESH;

    @Value("${arex.schedule.bizLog.sizeToSave}")
    private int LOG_SIZE_TO_SAVE_CHECK;

    @Value("${arex.schedule.bizLog.secondToSave}")
    private long LOG_TIME_GAP_TO_SAVE_CHECK_BY_SEC;


    private final RedisCancelMonitor cancelMonitor = new RedisCancelMonitor();
    private final ProgressMonitor progressMonitor = new ProgressMonitor();
    private final BizLoggerMonitor bizLoggerMonitor = new BizLoggerMonitor();

    @PostConstruct
    public void init() {
        Timer timer = new Timer("Execution_monitor_timer", true);
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

    @Override
    public void monitorOne(ReplayPlan task) {
        try {
            LOGGER.info("Monitoring task {}", task.getId());
            refreshLastUpdateTime(task);
            refreshCancelStatus(task);
            this.bizLoggerMonitor.tryFlushingLogs(task);
        } catch (Throwable t) {
            LOGGER.error("Monitor failed refreshing task " + task.getId(), t);
        }
    }

    @Override
    public void register(ReplayPlan plan) {
        tasks.put(plan.getId(), plan);
    }

    @Override
    public void deregister(ReplayPlan plan) {
        tasks.remove(plan.getId());
        this.bizLoggerMonitor.flushLogs(plan);
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

    private class BizLoggerMonitor {
        private void tryFlushingLogs(ReplayPlan replayPlan) {
            BlockingQueue<BizLog> logs = replayPlan.getBizLogs();
            long elapsed = System.currentTimeMillis() - replayPlan.getLastLogTime();
            boolean needFlush = logs.size() > LOG_SIZE_TO_SAVE_CHECK || elapsed > LOG_TIME_GAP_TO_SAVE_CHECK_BY_SEC * 1000L;
            if (needFlush) {
                flushLogs(replayPlan);
            }
        }

        private void flushLogs(ReplayPlan replayPlan) {
            try {
                BlockingQueue<BizLog> logs = replayPlan.getBizLogs();
                List<BizLog> logsToSave = new ArrayList<>();
                int curSize = replayPlan.getBizLogs().size();
                for (int i = 0; i < curSize; i++) {
                    logsToSave.add(logs.remove());
                }
                replayPlan.setLastLogTime(System.currentTimeMillis());
                replayBizLogRepository.saveAll(logsToSave);
            } catch (Throwable t) {
                LOGGER.error("Error flushing biz logs", t);
            }
        }
    }
}

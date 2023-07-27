package com.arextest.schedule.planexecution;

import com.arextest.common.cache.CacheProvider;
import com.arextest.schedule.dao.mongodb.ReplayBizLogRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanRepository;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.bizlog.BizLog;
import com.arextest.schedule.model.plan.PlanStageEnum;
import com.arextest.schedule.model.plan.ReplayPlanStageInfo;
import com.arextest.schedule.model.plan.StageStatusEnum;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.service.PlanProduceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by Qzmo on 2023/6/16
 */
@Component
@Slf4j
public class PlanExecutionMonitorImpl implements PlanExecutionMonitor {
    @Resource
    private CacheProvider redisCacheProvider;
    @Resource
    private ProgressTracer progressTracer;
    @Resource
    private ReplayBizLogRepository replayBizLogRepository;
    @Resource
    private ScheduledExecutorService monitorScheduler;
    @Resource
    private ReplayPlanRepository replayPlanRepository;

    @Value("${arex.schedule.monitor.secondToRefresh}")
    public int SECOND_TO_REFRESH;

    @Value("${arex.schedule.bizLog.sizeToSave}")
    private int LOG_SIZE_TO_SAVE_CHECK;

    @Value("${arex.schedule.bizLog.secondToSave}")
    private long LOG_TIME_GAP_TO_SAVE_CHECK_BY_SEC;

    private final ProgressMonitor progressMonitor = new ProgressMonitor();
    private final BizLoggerMonitor bizLoggerMonitor = new BizLoggerMonitor();
    private final RedisCancelMonitor redisCancelMonitor = new RedisCancelMonitor();
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
            MDCTracer.addPlanId(replayPlan.getId());
            try {
                monitorOne(replayPlan);
            } catch (Throwable t) {
                LOGGER.error("Error monitoring plan", t);
            } finally {
                MDCTracer.clear();
            }
        }
    }

    @Override
    public void monitorOne(ReplayPlan task) {
        if (task != null) {
            LOGGER.info("Monitoring task {}", task.getId());
            refreshLastUpdateTime(task);
            refreshCancelStatus(task);
            this.bizLoggerMonitor.tryFlushingLogs(task);

            if (!task.getReplayPlanStageList().equals(replayPlanStageListMap.get(task.getId()))) {
                replayPlanStageListMap.put(task.getId(), new ArrayList<>(task.getReplayPlanStageList()));
                replayPlanRepository.updateStage(task);
            }
        }

    }

    @Override
    public void register(ReplayPlan plan) {
        MonitorTask task = new MonitorTask(plan);
        ScheduledFuture<?> monitorFuture = monitorScheduler
                .scheduleAtFixedRate(task, 0, SECOND_TO_REFRESH, TimeUnit.SECONDS);
        plan.setMonitorFuture(monitorFuture);
    }

    @Override
    public void deregister(ReplayPlan plan) {
        plan.getMonitorFuture().cancel(false);
        LOGGER.info("deregister monitor task, planId: {}", plan.getId());

        this.bizLoggerMonitor.flushLogs(plan);
        replayPlanRepository.updateStage(plan);
    }

    private void refreshLastUpdateTime(ReplayPlan plan) {
        this.progressMonitor.refreshLastUpdateTime(plan.getId());
    }

    private void refreshCancelStatus(ReplayPlan plan) {
        if ((plan.getPlanStatus() != null && plan.getPlanStatus().isCanceled())
            || redisCancelMonitor.isPlanCanceled(plan)) {
            LOGGER.info("Plan {} cancel status set to true", plan.getId());
            addCancelStage(plan);
            plan.getPlanStatus().setCanceled(true);
            plan.getMonitorFuture().cancel(false);
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

                while (!logs.isEmpty()) {
                    logsToSave.add(logs.remove());
                }

                replayPlan.setLastLogTime(System.currentTimeMillis());
                replayBizLogRepository.saveAll(logsToSave);
            } catch (Throwable t) {
                LOGGER.error("Error flushing biz logs", t);
            }
        }
    }

    private class RedisCancelMonitor {
        boolean isPlanCanceled(ReplayPlan plan) {
            return isPlanCanceled(plan.getId());
        }
        private boolean isPlanCanceled(String planId) {
            return isCancelled(PlanProduceService.buildStopPlanRedisKey(planId));
        }

        private boolean isCancelled(byte[] redisKey) {
            return redisCacheProvider.get(redisKey) != null;
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

package com.arextest.schedule.planexecution.impl;

import com.arextest.schedule.dao.mongodb.ReplayBizLogRepository;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.bizlog.BizLog;
import com.arextest.schedule.planexecution.PlanMonitorHandler;
import com.arextest.schedule.progress.ProgressTracer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author wildeslam.
 * @create 2023/7/31 15:47
 */
@Component
@Order(1)
@Slf4j
public class LogHandler implements PlanMonitorHandler {
    @Resource
    private ProgressTracer progressTracer;
    @Resource
    private ReplayBizLogRepository replayBizLogRepository;
    @Resource
    private RedisCancelMonitor redisCancelMonitor;

    @Value("${arex.schedule.bizLog.sizeToSave}")
    private int LOG_SIZE_TO_SAVE_CHECK;

    @Value("${arex.schedule.bizLog.secondToSave}")
    private long LOG_TIME_GAP_TO_SAVE_CHECK_BY_SEC;

    private final ProgressMonitor progressMonitor = new ProgressMonitor();
    private final BizLoggerMonitor bizLoggerMonitor = new BizLoggerMonitor();

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

    @Override
    public void handle(ReplayPlan plan) {
        refreshLastUpdateTime(plan);
        refreshCancelStatus(plan);
        this.bizLoggerMonitor.tryFlushingLogs(plan);
    }

    @Override
    public void end(ReplayPlan plan) {
        this.bizLoggerMonitor.flushLogs(plan);
    }

    private void refreshLastUpdateTime(ReplayPlan plan) {
        this.progressMonitor.refreshLastUpdateTime(plan.getId());
    }

    private void refreshCancelStatus(ReplayPlan plan) {
        if (plan.getPlanStatus() == null) {
            return;
        }

        if (plan.getPlanStatus().isCanceled() || redisCancelMonitor.isPlanCanceled(plan)) {
            LOGGER.info("Plan {} cancel status set to true", plan.getId());
            plan.getPlanStatus().setCanceled(true);
            plan.getMonitorFuture().cancel(false);
        }
    }
}

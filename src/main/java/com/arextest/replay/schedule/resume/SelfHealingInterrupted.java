package com.arextest.replay.schedule.resume;

import com.arextest.replay.schedule.dao.ReplayActionCaseItemRepository;
import com.arextest.replay.schedule.dao.ReplayPlanActionRepository;
import com.arextest.replay.schedule.dao.ReplayPlanRepository;
import com.arextest.replay.schedule.mdc.MDCTracer;
import com.arextest.replay.schedule.plan.PlanContext;
import com.arextest.replay.schedule.plan.PlanContextCreator;
import com.arextest.replay.schedule.progress.ProgressEvent;
import com.arextest.replay.schedule.progress.ProgressTracer;
import com.arextest.replay.schedule.service.ConfigurationService;
import com.arextest.replay.schedule.service.PlanConsumeService;
import com.arextest.replay.schedule.service.PlanProduceService;
import com.arextest.replay.schedule.model.AppServiceOperationDescriptor;
import com.arextest.replay.schedule.model.ReplayActionCaseItem;
import com.arextest.replay.schedule.model.ReplayActionItem;
import com.arextest.replay.schedule.model.ReplayPlan;
import com.arextest.replay.schedule.utils.ReplayParentBinder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.List;

/**
 * @author jmo
 * @since 2021/10/12
 */
@Component
@Slf4j
public class SelfHealingInterrupted {
    @Resource
    private ReplayPlanRepository replayPlanRepository;
    @Resource
    private ReplayPlanActionRepository replayPlanActionRepository;
    @Resource
    private ReplayActionCaseItemRepository replayActionCaseItemRepository;
    @Resource
    private ConfigurationService configurationService;
    @Resource
    private ProgressEvent progressEvent;
    @Resource
    private ProgressTracer progressTracer;
    @Resource
    private PlanConsumeService planConsumeService;
    @Resource
    private PlanProduceService planProduceService;
    @Resource
    private PlanContextCreator planContextCreator;

    public void resumeTimeout(Duration offsetDuration, Duration maxDuration) {
        List<ReplayPlan> planList = replayPlanRepository.timeoutPlanList(offsetDuration, maxDuration);
        if (CollectionUtils.isEmpty(planList)) {
            return;
        }
        long durationMillis = offsetDuration.toMillis();
        for (ReplayPlan replayPlan : planList) {
            long planId = replayPlan.getId();
            MDCTracer.addPlanId(planId);
            try {
                if (isRunning(planId, durationMillis)) {
                    LOGGER.warn("skip resume when the plan running, plan id: {} , timeout millis {},", planId,
                            durationMillis);
                    continue;
                }
                doResume(replayPlan);
            } catch (Throwable throwable) {
                LOGGER.error("do resume plan error:{} ,plan id: {}", throwable.getMessage(), planId, throwable);
            }
        }
        MDCTracer.clear();
    }

    private void doResume(ReplayPlan replayPlan) {
        long planId = replayPlan.getId();
        List<ReplayActionItem> actionItems = replayPlanActionRepository.queryPlanActionList(planId);
        if (CollectionUtils.isEmpty(actionItems)) {
            LOGGER.warn("skip resume when the plan empty action list, plan id: {} mark to finish ", planId);
            progressEvent.onReplayPlanFinish(replayPlan);
            return;
        }
        if (isActionFinished(actionItems)) {
            LOGGER.warn("skip resume when the all actions finished, plan id: {} mark to plan finish ", planId);
            progressEvent.onReplayPlanFinish(replayPlan);
            return;
        }
        ConfigurationService.ScheduleConfiguration schedule =
                configurationService.schedule(replayPlan.getAppId());
        if (schedule != null) {
            replayPlan.setReplaySendMaxQps(schedule.getSendMaxQps());
        }
        replayPlan.setReplayActionItemList(actionItems);
        doResumeOperationDescriptor(replayPlan);
        doResumeLastRecordTime(actionItems);
        ReplayParentBinder.setupReplayActionParent(actionItems, replayPlan);
        LOGGER.info("try resume the plan running, plan id: {}", planId);
        planConsumeService.runAsyncConsume(replayPlan);
    }

    private void doResumeOperationDescriptor(ReplayPlan replayPlan) {
        PlanContext planContext = planContextCreator.createByAppId(replayPlan.getAppId());
        AppServiceOperationDescriptor operationDescriptor;
        for (ReplayActionItem actionItem : replayPlan.getReplayActionItemList()) {
            operationDescriptor = planContext.findAppServiceOperationDescriptor(actionItem.getOperationId());
            if (operationDescriptor == null) {
                LOGGER.warn("skip resume when the plan operationDescriptor not found, action id: {} ,",
                        actionItem.getId()
                );
                continue;
            }
            planContext.fillReplayAction(actionItem, operationDescriptor);
        }
    }

    private boolean isActionFinished(List<ReplayActionItem> actionItems) {
        for (ReplayActionItem actionItem : actionItems) {
            if (actionItem.finished()) {
                continue;
            }
            return false;
        }
        return true;
    }

    private void doResumeLastRecordTime(List<ReplayActionItem> actionItems) {
        for (ReplayActionItem actionItem : actionItems) {
            ReplayActionCaseItem lastCastItem = replayActionCaseItemRepository.lastOne(actionItem.getId());
            if (lastCastItem != null) {
                actionItem.setLastRecordTime(lastCastItem.getRecordTime());
            }
        }
    }

    private boolean isRunning(long planId, long timeoutMillis) {
        long now = System.currentTimeMillis();
        long lastUpdateTime = progressTracer.lastUpdateTime(planId);
        return (now - lastUpdateTime) < timeoutMillis;
    }
}

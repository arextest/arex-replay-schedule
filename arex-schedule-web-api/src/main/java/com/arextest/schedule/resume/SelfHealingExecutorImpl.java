package com.arextest.schedule.resume;

import com.arextest.schedule.bizlog.BizLogger;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanActionRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanRepository;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.AppServiceOperationDescriptor;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.AppServiceDescriptor;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.plan.PlanContext;
import com.arextest.schedule.plan.PlanContextCreator;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.service.ConfigurationService;
import com.arextest.schedule.service.DeployedEnvironmentService;
import com.arextest.schedule.service.PlanConsumeService;
import com.arextest.schedule.utils.ReplayParentBinder;
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
public class SelfHealingExecutorImpl implements SelfHealingExecutor {
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
    private PlanContextCreator planContextCreator;
    @Resource
    private DeployedEnvironmentService deployedEnvironmentService;

    // #TODO There is a problem here, Date and Duration types are compared
    public void defaultSelfHealing(Duration offsetDuration, Duration maxDuration) {
        List<ReplayPlan> timeoutPlans = queryTimeoutPlan(offsetDuration, maxDuration);
        if (CollectionUtils.isEmpty(timeoutPlans)) {
            return;
        }
        long durationMillis = offsetDuration.toMillis();
        for (ReplayPlan replayPlan : timeoutPlans) {
            String planId = replayPlan.getId();
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

    public List<ReplayPlan> queryTimeoutPlan(Duration offsetDuration, Duration maxDuration) {
        return replayPlanRepository.timeoutPlanList(offsetDuration, maxDuration);
    }

    public void doResume(ReplayPlan replayPlan) {
        long planCreateMillis = System.currentTimeMillis();
        replayPlan.setPlanCreateMillis(planCreateMillis);
        String planId = replayPlan.getId();
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
        replayPlan.setResumed(true);
        BizLogger.recordResumeRun(replayPlan);
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
            AppServiceDescriptor appServiceDescriptor = operationDescriptor.getParent();
            List<ServiceInstance> activeInstanceList = deployedEnvironmentService.getActiveInstanceList(appServiceDescriptor,
                    replayPlan.getTargetEnv());
            appServiceDescriptor.setTargetActiveInstanceList(activeInstanceList);
            
            planContext.fillReplayAction(actionItem, operationDescriptor);
        }
    }

    private boolean isActionFinished(List<ReplayActionItem> actionItems) {
        for (ReplayActionItem actionItem : actionItems) {
            if (actionItem.finalized()) {
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

    private boolean isRunning(String planId, long timeoutMillis) {
        long now = System.currentTimeMillis();
        long lastUpdateTime = progressTracer.lastUpdateTime(planId);
        return (now - lastUpdateTime) < timeoutMillis;
    }
}
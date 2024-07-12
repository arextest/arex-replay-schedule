package com.arextest.schedule.common;


import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.deploy.ServiceInstance;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * the rate limiter factory
 *
 * @author thji
 * @date 2024/7/23
 * @since 1.0.0
 */
@Slf4j
public class RateLimiterFactory {

    private static final int DEFAULT_INSTANCE_COUNT = 1;
    @Setter
    private int singleTasks;
    @Setter
    private int continuousFailThreshold;
    @Getter
    private int qpsPerInstance;

    private final double errorBreakRatio;

    private final Map<String, SendSemaphoreLimiter> serviceLimiterMap = new HashMap<>();

    public RateLimiterFactory(final double errorBreakRatio, final int continuousFailThreshold, final ReplayPlan replayPlan) {
        this.errorBreakRatio = errorBreakRatio;
        this.continuousFailThreshold = continuousFailThreshold;
        this.init(replayPlan);
    }

    /**
     * init the rate limiter factory
     *
     * @param replayPlan the replay plan
     */
    private void init(ReplayPlan replayPlan) {
        if (replayPlan == null) {
            return;
        }
        Optional<ReplayActionItem> optionalReplayActionItem = replayPlan.getReplayActionItemList()
                .stream()
                .filter(obj -> CollectionUtils.isNotEmpty(obj.getTargetInstance())).findFirst();
        if (!optionalReplayActionItem.isPresent()) {
            return;
        }
        List<ServiceInstance> targetInstances = optionalReplayActionItem.get().getTargetInstance();
        if (CollectionUtils.isEmpty(targetInstances)) {
            return;
        }
        this.qpsPerInstance = replayPlan.getReplaySendMaxQps();
        this.singleTasks = replayPlan.getCaseTotalCount() / targetInstances.size();

        for (ServiceInstance targetInstance : targetInstances) {
            buildRateLimiter(targetInstance.getIp());
        }
        replayPlan.setRateLimiterFactory(this);
        LOGGER.info("[[title=RateLimiterFactory]] init success for [{}]", serviceLimiterMap.values());
    }

    private void buildRateLimiter(String host) {
        if (StringUtils.isBlank(host)) {
            return;
        }
        serviceLimiterMap
                .computeIfAbsent(host,
                        k -> {
                            SendSemaphoreLimiter sendSemaphoreLimiter = new SendSemaphoreLimiter(qpsPerInstance, DEFAULT_INSTANCE_COUNT);
                            sendSemaphoreLimiter.setTotalTasks(singleTasks);
                            sendSemaphoreLimiter.setErrorBreakRatio(errorBreakRatio);
                            sendSemaphoreLimiter.setContinuousFailThreshold(continuousFailThreshold);
                            sendSemaphoreLimiter.setHost(host);
                            return sendSemaphoreLimiter;
                        });
    }

    /**
     * get the rate limiter by url
     *
     * @param host the host
     * @return the rate limiter
     */
    public SendSemaphoreLimiter get(String host) {
        if (StringUtils.isBlank(host)) {
            return null;
        }
        return serviceLimiterMap.get(host);
    }

    /**
     * get all the rate limiter
     *
     * @return the rate limiter collection
     */
    public Collection<SendSemaphoreLimiter> getAll() {
        return serviceLimiterMap.values();
    }

    @Override
    public String toString() {
        return "RateLimiterFactory{" +
                "singleTasks=" + singleTasks +
                ", continuousFailThreshold=" + continuousFailThreshold +
                ", qpsPerInstance=" + qpsPerInstance +
                ", errorBreakRatio=" + errorBreakRatio +
                ", serviceLimiterMap=" + serviceLimiterMap +
                '}';
    }
}

package com.arextest.schedule.common;

import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.deploy.ServiceInstance;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * the rate limiter factory
 *
 * @author thji
 * @date 2024/7/26
 * @since 1.0.0
 */
class RateLimiterFactoryTest {

    @Test
    void testCreateRateLimiter() {
        ReplayPlan replayPlan = new ReplayPlan();
        replayPlan.setCaseTotalCount(90);
        replayPlan.setReplaySendMaxQps(20);
        replayPlan.setReplayActionItemList(new ArrayList<>());
        ServiceInstance serviceInstance1 = new ServiceInstance();
        serviceInstance1.setIp("127.0.0.1");
        ServiceInstance serviceInstance2 = new ServiceInstance();
        serviceInstance2.setIp("127.0.0.2");
        ServiceInstance serviceInstance3 = new ServiceInstance();
        serviceInstance3.setIp("127.0.0.3");
        ReplayActionItem replayActionItem = new ReplayActionItem();
        replayActionItem.setTargetInstance(Arrays.asList(serviceInstance1, serviceInstance2, serviceInstance3));
        replayPlan.getReplayActionItemList().add(replayActionItem);
        RateLimiterFactory rateLimiterFactory = new RateLimiterFactory(0.1, 40, replayPlan);

        assertNotNull(rateLimiterFactory.get("127.0.0.1"));
        assertNotNull(rateLimiterFactory.get("127.0.0.2"));
        assertNotNull(rateLimiterFactory.get("127.0.0.3"));
    }

}

package com.arextest.schedule.model.plan;

import lombok.Getter;

/**
 * @author jmo
 * @since 2021/9/18
 */
public enum BuildReplayPlanType {
    /**
     * app维度回放
     */
    BY_APP_ID(0),
    /**
     * 接口维度回放
     */
    BY_OPERATION_OF_APP_ID(1),
    /**
     * 固定case回放
     */
    BY_FIXED_CASE(2),

    /**
     * rolling表中case回放
     */
    BY_ROLLING_CASE(3);

    @Getter
    final int value;

    BuildReplayPlanType(int value) {
        this.value = value;
    }
}
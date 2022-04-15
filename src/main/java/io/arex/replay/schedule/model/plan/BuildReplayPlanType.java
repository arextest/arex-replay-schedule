package io.arex.replay.schedule.model.plan;

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
    BY_FIXED_CASE(2);
    @Getter
    final int value;

    BuildReplayPlanType(int value) {
        this.value = value;
    }
}

package com.arextest.schedule.model.plan;

import lombok.Getter;

/**
 * Created by Qzmo on 2023/6/29
 *
 * @see <a href="https://github.com/arextest/arex/issues/224">Error Code desc</a>
 *
 */
public enum BuildReplayFailReasonEnum {
    /**
     * Normally created plan
     */
    NORMAL(0),
    /**
     * Another plan is creating for the same app
     */
    CREATING(1),
    /**
     * Invalid replay type
     * @see BuildReplayPlanType
     */
    INVALID_REPLAY_TYPE(2),
    /**
     * Can not find interface for the requested appid
     * Interfaces are auto-detected from the recorded cases
     */
    NO_INTERFACE_FOUND(101),
    /**
     * Invalid case source
     * NOT USED YET
     */
    INVALID_SOURCE_TYPE(102),
    /**
     * Invalid case range
     */
    INVALID_CASE_RANGE(103),
    /**
     * Cannot find any active instance with AREX agent
     */
    NO_ACTIVE_SERVICE_INSTANCE(104),
    /**
     * Cannot find any recorded case in the requested range
     */
    NO_CASE_IN_RANGE(200),
    /**
     * Schedule internal DB error
     */
    DB_ERROR(300),
    /**
     * Any other unexpected exceptions
     */
    UNKNOWN(500),
    ;
    @Getter
    private int code;
    BuildReplayFailReasonEnum(int code) {
        this.code = code;
    }
}
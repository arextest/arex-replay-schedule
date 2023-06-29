package com.arextest.schedule.model.plan;

import lombok.Getter;

/**
 * Created by Qzmo on 2023/6/29
 *
 * @see <a href="https://github.com/arextest/arex/issues/224">Error Code desc</a>
 *
 */
public enum BuildReplayFailReasonEnum {
    NORMAL(0),
    CREATING(1),
    INVALID_REPLAY_TYPE(2),
    NO_INTERFACE_FOUND(101),
    INVALID_SOURCE_TYPE(102),
    INVALID_CASE_RANGE(103),
    NO_ACTIVE_SERVICE_INSTANCE(104),
    NO_CASE_IN_RANGE(200),
    DB_ERROR(300),
    UNKNOWN(500),
    ;
    @Getter
    private int code;
    BuildReplayFailReasonEnum(int code) {
        this.code = code;
    }
}
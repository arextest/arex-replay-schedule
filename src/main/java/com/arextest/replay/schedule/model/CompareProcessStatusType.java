package com.arextest.replay.schedule.model;

import lombok.Getter;

/**
 * @author jmo
 * @since 2021/9/15
 */
public enum CompareProcessStatusType {
    WAIT_HANDLING(0),
    ERROR(-1),
    PASS(1),
    HAS_DIFF(2);
    @Getter
    final int value;

    CompareProcessStatusType(int value) {
        this.value = value;
    }
}

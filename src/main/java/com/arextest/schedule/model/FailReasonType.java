package com.arextest.schedule.model;

import lombok.Getter;

/**
 * @author jmo
 * @since 2021/9/15
 */
public enum FailReasonType {
    COMPARE_FAIL(0),
    SEND_FAIL(1),
    OTHER(2),
    ;
    @Getter
    final int value;

    FailReasonType(int value) {
        this.value = value;
    }
}
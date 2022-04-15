package io.arex.replay.schedule.model;

import lombok.Getter;

/**
 * @author jmo
 * @since 2021/10/8
 */
public enum ReplayStatusType {
    INIT(0),
    RUNNING(1),
    FINISHED(2),
    FAIL_INTERRUPTED(3),
    CANCELLED(4);
    @Getter
    final int value;

    ReplayStatusType(int value) {
        this.value = value;
    }
}

package com.arextest.schedule.model;

import lombok.Getter;

/**
 * @author jmo
 * @since 2021/10/8
 */
public enum ReplayStatusType {
    /**
     * Initial status
     */
    INIT(0),
    /**
     * Case loaded to ReplayRunDetails collection, awaiting sending
     */
    CASE_LOADED(5),
    /**
     * Processing(send, compare) cases
     */
    RUNNING(1),
    /**
     * Finished normally
     */
    FINISHED(2),
    /**
     * Reaching failed conditions, no need to continue
     * @see com.arextest.schedule.common.SendSemaphoreLimiter
     */
    FAIL_INTERRUPTED(3),
    /**
     * Cancelled by user
     */
    CANCELLED(4),

    /**
     * Re-run plan
     */
    RERUNNING(6),
    ;

    @Getter
    final int value;

    ReplayStatusType(int value) {
        this.value = value;
    }

    public boolean finalized() {
        return this == FINISHED || this == FAIL_INTERRUPTED || this == CANCELLED;
    }

    public static ReplayStatusType ofCode(int code) {
        // return corresponding enum value, or INIT if not found
        switch (code) {
            case 0:
                return INIT;
            case 1:
                return RUNNING;
            case 2:
                return FINISHED;
            case 3:
                return FAIL_INTERRUPTED;
            case 4:
                return CANCELLED;
            case 5:
                return CASE_LOADED;
            case 6:
                return RERUNNING;
            default:
                return INIT;
        }
    }
}
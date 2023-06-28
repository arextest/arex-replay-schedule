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
     * Processing cases
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
    CANCELLED(4);

    @Getter
    final int value;

    ReplayStatusType(int value) {
        this.value = value;
    }
}
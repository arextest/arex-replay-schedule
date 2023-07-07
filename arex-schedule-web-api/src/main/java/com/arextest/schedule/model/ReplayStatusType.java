package com.arextest.schedule.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
    CANCELLED(4);

    @Getter
    final int value;

    ReplayStatusType(int value) {
        this.value = value;
    }

    public boolean finalized() {
        return this == FINISHED || this == FAIL_INTERRUPTED || this == CANCELLED;
    }

    public static ReplayStatusType ofCode(int code) {
        for (ReplayStatusType status : ReplayStatusType.values()) {
            if (code == status.getValue()) {
                return status;
            }
        }
        return INIT;
    }
}
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

    public static final Set<ReplayStatusType> FINALISED = Sets.newHashSet(FINISHED, FAIL_INTERRUPTED, CANCELLED);
    public static final Set<Integer> FINALISED_CODE = FINALISED.stream().map(ReplayStatusType::getValue)
            .collect(Collectors.toSet());

    @Getter
    final int value;

    ReplayStatusType(int value) {
        this.value = value;
    }
}
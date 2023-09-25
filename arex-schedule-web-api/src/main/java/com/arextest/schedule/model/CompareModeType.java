package com.arextest.schedule.model;

import lombok.Getter;

public enum CompareModeType {

    /**
     * quick compare：0
     */
    QUiCK(0),

    /**
     * full compare：1
     */
    FULL(1);

    @Getter
    private final int value;

    CompareModeType(int value) {
        this.value = value;
    }
}

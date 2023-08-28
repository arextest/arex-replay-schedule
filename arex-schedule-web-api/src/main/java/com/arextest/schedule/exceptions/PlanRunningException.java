package com.arextest.schedule.exceptions;

import lombok.Getter;

/**
 * Created by Qzmo on 2023/7/27
 */
public class PlanRunningException extends Exception {
    @Getter
    private final int code;
    @Getter
    private final String message;

    public PlanRunningException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}

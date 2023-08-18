package com.arextest.schedule.exceptions;

import lombok.Getter;

/**
 * @author wildeslam.
 * @create 2023/8/15 16:35
 */
public class ReRunPlanException extends Exception {
    @Getter
    private final int code;
    @Getter
    private final String message;

    public ReRunPlanException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}

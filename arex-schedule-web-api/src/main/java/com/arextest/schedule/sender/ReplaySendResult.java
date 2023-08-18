package com.arextest.schedule.sender;

import com.arextest.schedule.model.CaseSendStatusType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.Map;

/**
 * @author jmo
 * @since 2021/9/16
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public final class ReplaySendResult {
    private final String traceId;
    private final Throwable exception;
    private final String url;
    private final Map<String, String> headers;
    private final CaseSendStatusType statusType;

    public boolean success() {
        return statusType == CaseSendStatusType.SUCCESS;
    }

    private ReplaySendResult(String traceId, Throwable exception, String url, CaseSendStatusType statusType) {
        this.traceId = traceId;
        this.exception = exception;
        this.url = url;
        this.headers = Collections.emptyMap();
        this.statusType = statusType;
    }

    public static ReplaySendResult success(String traceId, String url) {
        return new ReplaySendResult(traceId, null, url, CaseSendStatusType.SUCCESS);
    }

    public static ReplaySendResult success(String traceId, String url, Map<String, String> headers) {
        return new ReplaySendResult(traceId, null, url, headers, CaseSendStatusType.SUCCESS);
    }

    public static ReplaySendResult failed(Throwable exception) {
        return new ReplaySendResult(null, exception, null, CaseSendStatusType.EXCEPTION_FAILED);
    }

    public static ReplaySendResult failed(Throwable exception, String url) {
        return new ReplaySendResult(null, exception, url, CaseSendStatusType.EXCEPTION_FAILED);
    }

    public static ReplaySendResult failed(Throwable exception, String url, Map<String, String> headers) {
        return new ReplaySendResult(null, exception, url, headers, CaseSendStatusType.EXCEPTION_FAILED);
    }
}
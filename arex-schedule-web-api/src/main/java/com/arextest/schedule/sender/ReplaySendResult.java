package com.arextest.schedule.sender;

import com.arextest.schedule.model.CaseSendStatusType;
import java.util.Collections;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * @author jmo
 * @since 2021/9/16
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public final class ReplaySendResult {

  private final String traceId;
  private final String remark;
  private final String url;
  private final Map<String, String> headers;
  private final CaseSendStatusType statusType;

  private ReplaySendResult(String traceId, String remark, String url,
      CaseSendStatusType statusType) {
    this.traceId = traceId;
    this.remark = remark;
    this.url = url;
    this.headers = Collections.emptyMap();
    this.statusType = statusType;
  }

  public static ReplaySendResult success(String traceId, String remark, String url) {
    return new ReplaySendResult(traceId, remark, url, CaseSendStatusType.SUCCESS);
  }

  public static ReplaySendResult success(String traceId, String remark, String url,
      Map<String, String> headers) {
    return new ReplaySendResult(traceId, remark, url, headers, CaseSendStatusType.SUCCESS);
  }

  public static ReplaySendResult failed(String remark) {
    return new ReplaySendResult(null, remark, null, CaseSendStatusType.EXCEPTION_FAILED);
  }

  public static ReplaySendResult failed(String remark, String url) {
    return new ReplaySendResult(null, remark, url, CaseSendStatusType.EXCEPTION_FAILED);
  }

  public static ReplaySendResult failed(String remark, String url, Map<String, String> headers) {
    return new ReplaySendResult(null, remark, url, headers, CaseSendStatusType.EXCEPTION_FAILED);
  }

  public boolean success() {
    return statusType == CaseSendStatusType.SUCCESS;
  }
}
package com.arextest.schedule.sender.impl;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.model.LogType;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.sender.ReplaySendResult;
import com.arextest.schedule.sender.ReplaySenderParameters;
import com.arextest.schedule.sender.SenderParameters;
import com.arextest.schedule.service.MetricService;
import com.arextest.schedule.utils.DecodeUtils;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Slf4j
@Component
public final class DefaultHttpReplaySender extends AbstractReplaySender {
  @Resource
  private HttpWepServiceApiClient httpWepServiceApiClient;
  @Resource
  private MetricService metricService;

  @Value("#{'${arex.replay.header.excludes.http}'.split(',')}")
  List<String> headerExcludes;

  @Override
  public boolean isSupported(String category) {
    return MockCategoryType.SERVLET.getName().equals(category)
        || MockCategoryType.NETTY_PROVIDER.getName().equals(category);
  }

  @Override
  public int getOrder() {
    return -1;
  }

  private boolean doSend(ReplayActionItem replayActionItem, ReplayActionCaseItem caseItem,
      Map<String, String> headers) {
    headerExcludes.forEach(headers::remove);

    ServiceInstance instanceRunner = selectLoadBalanceInstance(caseItem.getId(),
        replayActionItem.getTargetInstance());
    if (instanceRunner == null) {
      LOGGER.error("selectLoadBalanceInstance failed, caseItem:{}", caseItem);
      return false;
    }
    String operationName = caseItem.requestPath();
    if (StringUtils.isEmpty(operationName)) {
      operationName = replayActionItem.getOperationName();
    }
    ReplaySendResult targetSendResult;
    ReplaySenderParameters senderParameter = new ReplaySenderParameters();
    senderParameter.setAppId(replayActionItem.getAppId());
    senderParameter.setConsumeGroup(caseItem.consumeGroup());
    senderParameter.setFormat(caseItem.requestMessageFormat());
    senderParameter.setMessage(caseItem.requestMessage());
    senderParameter.setOperation(operationName);
    senderParameter.setUrl(instanceRunner.getUrl());
    senderParameter.setSubEnv(instanceRunner.subEnv());
    senderParameter.setHeaders(headers);
    senderParameter.setMethod(caseItem.requestMethod());
    senderParameter.setRecordId(caseItem.getRecordId());
    //todo get log message id and will optimize it later.
    String messageId = metricService.generateMessageIdEvent(headers, instanceRunner.getUrl());
    StopWatch watch = new StopWatch();
    watch.start(LogType.DO_SEND.getValue());
    targetSendResult = this.doInvoke(senderParameter);
    watch.stop();
    caseItem.setMessageId(messageId);
    metricService.recordSendLogEvent(LogType.DO_SEND.getValue(), targetSendResult, caseItem,
        watch.getTotalTimeMillis());
    caseItem.setSendErrorMessage(targetSendResult.getRemark());
    caseItem.setTargetResultId(targetSendResult.getTraceId());
    caseItem.setSendStatus(targetSendResult.getStatusType().getValue());
    instanceRunner = selectLoadBalanceInstance(caseItem.getId(),
        replayActionItem.getSourceInstance());
    if (instanceRunner == null) {
      return targetSendResult.success();
    }
    // the sourceHost sending
    senderParameter.setUrl(instanceRunner.getUrl());
    ReplaySendResult sourceSendResult = this.doInvoke(senderParameter);
    caseItem.setSourceResultId(sourceSendResult.getTraceId());
    caseItem.setSendStatus(sourceSendResult.getStatusType().getValue());
    caseItem.setSendErrorMessage(targetSendResult.getRemark());
    return sourceSendResult.success() && targetSendResult.success();
  }

  @Override
  public boolean send(ReplayActionCaseItem caseItem) {
    Map<String, String> headers = createHeaders(caseItem);
    ReplayActionItem replayActionItem = caseItem.getParent();
    before(caseItem, replayActionItem.getParent().getReplayPlanType());
    return doSend(replayActionItem, caseItem, headers);
  }

  @Override
  public boolean send(ReplayActionCaseItem caseItem, Map<String, String> extraHeaders) {
    Map<String, String> headers = createHeaders(caseItem);
    headers.putAll(extraHeaders);
    ReplayActionItem replayActionItem = caseItem.getParent();
    before(caseItem, replayActionItem.getParent().getReplayPlanType());
    return doSend(replayActionItem, caseItem, headers);
  }

  private String contactUrl(String baseUrl, String operation) {
    String result = null;
    if (StringUtils.endsWith(baseUrl, "/") || StringUtils.startsWith(operation, "/")) {
      result = baseUrl + operation;
    } else {
      result = baseUrl + "/" + operation;
    }
    return result;
  }

  private ReplaySendResult doInvoke(SenderParameters senderParameters) {
    String method = senderParameters.getMethod();
    HttpMethod httpMethod = HttpMethod.resolve(method);

    if (httpMethod == null) {
      return ReplaySendResult.failed("not found request method:" + method);
    }

    HttpHeaders httpHeaders = createRequestHeaders(senderParameters.getHeaders(),
        senderParameters.getFormat());
    String requestMessage = senderParameters.getMessage();
    String fullUrl =
        contactUrl(senderParameters.getUrl(), senderParameters.getOperation());
    Class<?> responseType = String.class;
    final HttpEntity<?> httpEntity;
    if (shouldApplyHttpBody(httpMethod)) {
      Object decodeMessage = DecodeUtils.decode(requestMessage);
      if (byte[].class == decodeMessage.getClass()) {
        responseType = byte[].class;
      }
      httpEntity = new HttpEntity<>(decodeMessage, httpHeaders);
    } else {
      httpEntity = new HttpEntity<>(httpHeaders);
    }
    final ResponseEntity<?> responseEntity;
    try {
      responseEntity = httpWepServiceApiClient.exchange(fullUrl, httpMethod, httpEntity,
          responseType);
    } catch (Throwable throwable) {
      LOGGER.error("http {} , url: {} ,error: {} ,request: {}", method, fullUrl,
          throwable.getMessage(),
          httpEntity,
          throwable);
      return ReplaySendResult.failed(throwable.getMessage(), fullUrl);
    }
    return fromHttpResult(httpHeaders, fullUrl, responseEntity);
  }

  private HttpHeaders createRequestHeaders(Map<String, String> sourceHeaders, String format) {
    MediaType contentType = null;
    HttpHeaders httpHeaders = new HttpHeaders();
    if (MapUtils.isNotEmpty(sourceHeaders)) {
      for (Map.Entry<String, String> entry : sourceHeaders.entrySet()) {
        String key = entry.getKey();
        if (contentType == null) {
          contentType = contentType(key, entry.getValue());
          if (contentType != null) {
            continue;
          }
        }
        httpHeaders.add(key, entry.getValue());
      }
    }
    if (contentType == null) {
      contentType = contentType(format);
    }
    httpHeaders.setContentType(contentType);
    return httpHeaders;
  }

  private boolean shouldApplyHttpBody(HttpMethod httpMethod) {
    return httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT
        || httpMethod == HttpMethod.PATCH ||
        httpMethod == HttpMethod.DELETE;
  }

  private MediaType contentType(String key, String value) {
    if (StringUtils.isEmpty(value)) {
      return null;
    }
    if (!StringUtils.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE, key)) {
      return null;
    }
    return MediaType.parseMediaType(value);
  }

  private MediaType contentType(String format) {
    if (StringUtils.isEmpty(format)) {
      return MediaType.APPLICATION_JSON;
    }
    return MediaType.parseMediaType(format);
  }


  private ReplaySendResult fromHttpResult(Map<?, ?> requestHeaders, String url,
      ResponseEntity<?> responseEntity) {
    HttpHeaders responseHeaders = null;
    Object responseBody = null;
    if (responseEntity != null) {
      responseHeaders = responseEntity.getHeaders();
      responseBody = responseEntity.getBody();
    }
    return fromResult(requestHeaders, url, responseHeaders, responseBody);
  }

  private ReplaySendResult fromResult(Map<?, ?> requestHeaders, String url,
      Map<?, ?> responseHeaders,
      Object responseBody) {
    String body = encodeResponseAsString(responseBody);
    LOGGER.info("invoke result url:{} ,request header:{},response header: {}", url,
        requestHeaders,
        responseHeaders);
    if (responseHeaders == null) {
      LOGGER.error("invoke result url:{} ,request header:{}, body: {}", url,
          requestHeaders,
          body
      );
      return ReplaySendResult.failed("replay post error,review log find more details", url);
    }
    if (!isReplayRequest(requestHeaders)) {
      return ReplaySendResult.success(StringUtils.EMPTY, StringUtils.EMPTY, url);
    }
    String resultId = replayResultId(responseHeaders);
    if (StringUtils.isEmpty(resultId)) {
      LOGGER.error("invoke result url:{} ,request header:{},response header: {}, body: {}", url,
          requestHeaders,
          responseHeaders,
          body
      );
      return ReplaySendResult.failed(
          "Could not fetch replay result id from the headers of response", url);
    }
    return ReplaySendResult.success(resultId, StringUtils.EMPTY, url);
  }


  private String replayResultId(Map<?, ?> responseHeaders) {
    if (MapUtils.isEmpty(responseHeaders)) {
      return null;
    }
    if (responseHeaders instanceof HttpHeaders) {
      return ((HttpHeaders) responseHeaders).getFirst(CommonConstant.AREX_REPLAY_ID);
    }
    Object value = responseHeaders.get(CommonConstant.AREX_REPLAY_ID);
    if (value instanceof String) {
      return (String) value;
    }
    return null;
  }
}
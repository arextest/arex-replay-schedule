package com.arextest.schedule.sender.impl;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.model.LogType;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.model.sender.HttpSenderContent;
import com.arextest.schedule.sender.ReplaySendResult;
import com.arextest.schedule.sender.ReplaySenderParameters;
import com.arextest.schedule.sender.SenderParameters;
import com.arextest.schedule.sender.httprequest.AbstractHttpRequestBuilder;
import com.arextest.schedule.sender.httprequest.HttpRequestBuilderFactory;
import com.arextest.schedule.service.MetricService;
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
  @Resource
  HttpRequestBuilderFactory httpRequestBuilderFactory;

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

    ServiceInstance instanceRunner = selectLoadBalanceInstance(caseItem,
        replayActionItem.getTargetInstance(), true);
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
    instanceRunner = selectLoadBalanceInstance(caseItem,
        replayActionItem.getSourceInstance(), false);
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
    before(caseItem);
    return doSend(replayActionItem, caseItem, headers);
  }

  @Override
  public boolean send(ReplayActionCaseItem caseItem, Map<String, String> extraHeaders) {
    Map<String, String> headers = createHeaders(caseItem);
    headers.putAll(extraHeaders);
    ReplayActionItem replayActionItem = caseItem.getParent();
    before(caseItem);
    return doSend(replayActionItem, caseItem, headers);
  }

  private ReplaySendResult doInvoke(SenderParameters senderParameters) {
    AbstractHttpRequestBuilder httpRequestBuilder = httpRequestBuilderFactory.getHttpRequestBuilder(
        senderParameters);
    if (httpRequestBuilder == null) {
      LOGGER.error("not found request content builder, senderParameters:{}", senderParameters);
      return ReplaySendResult.failed("not found request content builder");
    }

    HttpSenderContent httpSenderContent = httpRequestBuilder.buildRequestContent(senderParameters);
    if (httpSenderContent == null) {
      LOGGER.error("build request content failed, senderParameters:{}", senderParameters);
      return ReplaySendResult.failed("build request content failed");
    }

    if (httpSenderContent.getHttpMethod() == null) {
      return ReplaySendResult.failed("not found request method:" + senderParameters.getMethod());
    }

    String fullUrl = httpSenderContent.getRequestUrl();
    HttpMethod httpMethod = httpSenderContent.getHttpMethod();
    HttpEntity<?> httpEntity = httpSenderContent.getHttpEntity();
    Class<?> responseType = httpSenderContent.getResponseType();

    final ResponseEntity<?> responseEntity;
    try {
      responseEntity = httpWepServiceApiClient.exchange(fullUrl, httpMethod, httpEntity,
          responseType);
    } catch (Throwable throwable) {
      LOGGER.error("http {} , url: {} ,error: {} ,request: {}", senderParameters.getMethod(),
          fullUrl,
          throwable.getMessage(),
          httpEntity,
          throwable);
      return ReplaySendResult.failed(throwable.getMessage(), fullUrl);
    }
    return fromHttpResult(httpEntity.getHeaders(), fullUrl, responseEntity);
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
package com.arextest.replay.schedule.sender.impl;

import com.arextest.replay.schedule.sender.ReplaySendResult;
import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.replay.schedule.client.HttpWepServiceApiClient;
import com.arextest.replay.schedule.model.ReplayActionCaseItem;
import com.arextest.replay.schedule.model.ReplayActionItem;
import com.arextest.replay.schedule.model.deploy.ServiceInstance;
import com.arextest.replay.schedule.sender.ReplaySenderParameters;
import com.arextest.replay.schedule.sender.SenderParameters;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.arextest.replay.schedule.common.CommonConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author jmo
 * @since 2021/9/16
 */
@Slf4j
@Component
final class DefaultHttpServletReplaySender extends AbstractReplaySender {
    @Resource
    private HttpWepServiceApiClient httpWepServiceApiClient;
    @Resource
    private ObjectMapper objectMapper;

    private static final String PATTERN_STRING = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$";
    private static final Pattern BASE_64_PATTERN = Pattern.compile(PATTERN_STRING);

    @Override
    public boolean isSupported(int sendType) {
        return MockCategoryType.SERVLET_ENTRANCE.getCodeValue() == sendType;
    }

    @Override
    public boolean prepareRemoteDependency(ReplayActionCaseItem caseItem) {
        Map<String, String> headers = newHeadersIfEmpty(caseItem.getRequestHeaders());
        headers.put(CommonConstant.CONFIG_VERSION_HEADER_NAME, caseItem.getReplayDependency());
        return doSend(caseItem.getParent(), caseItem, headers) || doSend(caseItem.getParent(), caseItem, headers);
    }

    @Override
    public boolean activeRemoteService(ReplayActionCaseItem caseItem) {
        Map<String, String> headers = newHeadersIfEmpty(caseItem.getRequestHeaders());
        headers.put(CommonConstant.AREX_REPLAY_WARM_UP, Boolean.TRUE.toString());
        return doSend(caseItem.getParent(), caseItem, headers);
    }

    @Override
    public ReplaySendResult send(SenderParameters senderParameters) {
        if (StringUtils.isBlank(senderParameters.getUrl())) {
            return ReplaySendResult.failed("url is null or empty");
        }
        before(senderParameters.getRecordId());
        return doInvoke(senderParameters);
    }

    private Map<String, String> newHeadersIfEmpty(Map<String, String> source) {
        if (MapUtils.isEmpty(source)) {
            return new HashMap<>();
        }
        return source;
    }

    private boolean doSend(ReplayActionItem replayActionItem, ReplayActionCaseItem caseItem,
                           Map<String, String> headers) {
        ServiceInstance instanceRunner = replayActionItem.getTargetInstance();
        if (instanceRunner == null) {
            return false;
        }
        String operationName = caseItem.getRequestPath();
        if (StringUtils.isEmpty(operationName)) {
            operationName = replayActionItem.getOperationName();
        }
        ReplaySendResult targetSendResult;
        ReplaySenderParameters senderParameter = new ReplaySenderParameters();
        senderParameter.setAppId(replayActionItem.getAppId());
        senderParameter.setConsumeGroup(caseItem.getConsumeGroup());
        senderParameter.setFormat(caseItem.getRequestMessageFormat());
        senderParameter.setMessage(caseItem.getRequestMessage());
        senderParameter.setOperation(operationName);
        senderParameter.setUrl(instanceRunner.getUrl());
        senderParameter.setSubEnv(instanceRunner.subEnv());
        senderParameter.setHeaders(headers);
        senderParameter.setMethod(caseItem.getRequestMethod());
        senderParameter.setRecordId(caseItem.getRecordId());
        targetSendResult = this.doInvoke(senderParameter);
        caseItem.setSendErrorMessage(targetSendResult.getRemark());
        caseItem.setTargetResultId(targetSendResult.getTraceId());
        caseItem.setSendStatus(targetSendResult.getStatusType().getValue());
        instanceRunner = replayActionItem.getSourceInstance();
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
        Map<String, String> headers = newHeadersIfEmpty(caseItem.getRequestHeaders());
        ReplayActionItem replayActionItem = caseItem.getParent();
        before(caseItem.getRecordId());
        headers.remove(CommonConstant.AREX_REPLAY_WARM_UP);
        headers.put(CommonConstant.AREX_RECORD_ID, caseItem.getRecordId());
        return doSend(replayActionItem, caseItem, headers);
    }

    private String contactUrl(String baseUrl, String operation) {
        if (StringUtils.endsWith(baseUrl, "/") || StringUtils.startsWith(operation, "/")) {
            return baseUrl + operation;
        }
        return baseUrl + "/" + operation;
    }

    private ReplaySendResult doInvoke(SenderParameters senderParameters) {
        String fullUrl = contactUrl(senderParameters.getUrl(), senderParameters.getOperation());
        String method = senderParameters.getMethod();
        HttpMethod httpMethod = HttpMethod.resolve(method);
        if (httpMethod == null) {
            return ReplaySendResult.failed("not found request method:" + method, fullUrl);
        }
        HttpHeaders httpHeaders = createRequestHeaders(senderParameters.getHeaders(), senderParameters.getFormat());
        String requestMessage = senderParameters.getMessage();
        Class<?> responseType = String.class;
        final HttpEntity<?> httpEntity;
        if (shouldApplyHttpBody(httpMethod)) {
            Object decodeMessage = decode(requestMessage);
            if (byte[].class == decodeMessage.getClass()) {
                responseType = byte[].class;
            }
            httpEntity = new HttpEntity<>(decodeMessage, httpHeaders);
        } else {
            httpEntity = new HttpEntity<>(httpHeaders);
        }
        final ResponseEntity<?> responseEntity;
        try {
            responseEntity = httpWepServiceApiClient.exchange(fullUrl, httpMethod, httpEntity, responseType);
        } catch (Throwable throwable) {
            LOGGER.error("http {} , url: {} ,error: {} ,request: {}", method, fullUrl, throwable.getMessage(),
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

    private Object decode(String requestMessage) {
        if (BASE_64_PATTERN.matcher(requestMessage).matches()) {
            return Base64.getDecoder().decode(requestMessage);
        }
        return requestMessage;
    }

    private boolean shouldApplyHttpBody(HttpMethod httpMethod) {
        return httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT || httpMethod == HttpMethod.PATCH ||
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

    private boolean isReplayRequest(Map<?, ?> requestHeaders) {
        return MapUtils.isNotEmpty(requestHeaders) && requestHeaders.containsKey(CommonConstant.AREX_RECORD_ID);
    }

    private ReplaySendResult fromHttpResult(Map<?, ?> requestHeaders, String url, ResponseEntity<?> responseEntity) {
        HttpHeaders responseHeaders = null;
        Object responseBody = null;
        if (responseEntity != null) {
            responseHeaders = responseEntity.getHeaders();
            responseBody = responseEntity.getBody();
        }
        return fromResult(requestHeaders, url, responseHeaders, responseBody);
    }

    private ReplaySendResult fromResult(Map<?, ?> requestHeaders, String url, Map<?, ?> responseHeaders,
                                        Object responseBody) {
        String body = encodeResponseAsString(responseBody);
        LOGGER.info("invoke result url:{} ,request header:{},response header:{}, body:{}", url, requestHeaders,
                responseHeaders, body);
        if (responseHeaders == null) {
            return ReplaySendResult.failed("replay post error,review log find more details", url);
        }
        if (!isReplayRequest(requestHeaders)) {
            return ReplaySendResult.success(StringUtils.EMPTY, StringUtils.EMPTY, url);
        }
        String resultId = replayResultId(responseHeaders);
        if (StringUtils.isEmpty(resultId)) {
            return ReplaySendResult.failed("Could not fetch replay result id from the headers of response", url);
        }
        return ReplaySendResult.success(resultId, StringUtils.EMPTY, url);
    }

    private String encodeResponseAsString(Object responseBody) {
        if (responseBody == null) {
            return null;
        }
        if (responseBody instanceof String) {
            return (String) responseBody;
        }
        if (responseBody instanceof byte[]) {
            return Base64.getEncoder().encodeToString((byte[]) responseBody);
        }
        try {
            return objectMapper.writeValueAsString(responseBody);
        } catch (JsonProcessingException e) {
            LOGGER.warn("encodeAsString error:{}", e.getMessage(), e);
        }
        return null;
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

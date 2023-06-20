package com.arextest.schedule.sender.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.schedule.common.ClassLoaderUtils;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.converter.ReplayActionCaseItemConverter;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.sender.ReplaySendResult;
import com.arextest.schedule.sender.SenderParameters;
import com.arextest.schedule.spi.ReplaySenderExtension;
import com.arextest.schedule.spi.model.DubboRequest;
import com.arextest.schedule.spi.model.ReplayInvokeResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * @author b_yu
 * @since 2023/4/11
 */
@Slf4j
@Component
public class DubboReplaySender extends AbstractReplaySender {
    private static final String JAR_FILE_PATH = "D:\\Users\\yushuwang\\work\\lib\\dubboInvoker-1.0-SNAPSHOT-jar-with-dependencies.jar";
    @Override
    public boolean isSupported(String categoryType) {
        return MockCategoryType.DUBBO_PROVIDER.getName().equals(categoryType);
    }
    @Override
    public boolean send(ReplayActionCaseItem caseItem) {
        ReplayActionItem replayActionItem = caseItem.getParent();
        before(caseItem.getRecordId(), replayActionItem.getParent().getReplayPlanType());
        Map<String, String> headers = newHeadersIfEmpty(caseItem.requestHeaders());
        headers.remove(CommonConstant.AREX_REPLAY_WARM_UP);
        headers.put(CommonConstant.AREX_RECORD_ID, caseItem.getRecordId());
        headers.put(CommonConstant.AREX_SCHEDULE_REPLAY, Boolean.TRUE.toString());
        String exclusionOperationConfig = replayActionItem.getExclusionOperationConfig();
        if (StringUtils.isNotEmpty(exclusionOperationConfig)) {
            headers.put(CommonConstant.X_AREX_EXCLUSION_OPERATIONS, exclusionOperationConfig);
        }
        return doSend(caseItem, headers);
    }

    @Override
    public ReplaySendResult send(SenderParameters senderParameters) {
        return null;
    }

    DubboRequest generateDubboRequest(ReplayActionCaseItem caseItem) {
        DubboRequest dubboRequest = ReplayActionCaseItemConverter.INSTANCE.convertDubboRequest(caseItem);

        ImmutablePair<String, String> interfaceNameAndMethod =
                getInterfaceNameAndMethod(caseItem.getParent().getOperationName());
        if (interfaceNameAndMethod == null) {
            return null;
        }
        ServiceInstance instanceRunner = selectLoadBalanceInstance(caseItem.getId(), caseItem.getParent().getTargetInstance());
        if (instanceRunner == null) {
            return null;
        }
        String url = instanceRunner.getUrl();
        dubboRequest.setUrl(url);

        DubboParameters dubboParameters = getDubboParameters(caseItem);
        dubboRequest.setParameters(dubboParameters.getParameters());
        dubboRequest.setParameterTypes(dubboParameters.getParameterTypes());

        dubboRequest.setInterfaceName(interfaceNameAndMethod.left);
        dubboRequest.setMethodName(interfaceNameAndMethod.right);
        return dubboRequest;
    }

    private boolean doSend(ReplayActionCaseItem caseItem, Map<String, String> headers) {

        ReplayInvokeResult replayInvokeResult = null;

        DubboRequest dubboRequest = generateDubboRequest(caseItem);
        if (dubboRequest == null) {
            return false;
        }
        dubboRequest.setHeaders(headers);
        //ClassLoaderUtils.loadJar(JAR_FILE_PATH);
        ServiceLoader<ReplaySenderExtension> loader = ServiceLoader.load(ReplaySenderExtension.class);
        for (ReplaySenderExtension sender : loader) {
            if (sender.getName().equalsIgnoreCase("DefaultDubbo")) {
                replayInvokeResult = sender.invoke(dubboRequest);
            }
        }
        if (replayInvokeResult == null) {
            return false;
        }
        ReplaySendResult targetSendResult = fromDubboResult(headers, dubboRequest.getUrl(),
                replayInvokeResult.getResult(), replayInvokeResult.getReplayId());
        caseItem.setSendErrorMessage(targetSendResult.getRemark());
        caseItem.setTargetResultId(targetSendResult.getTraceId());
        caseItem.setSendStatus(targetSendResult.getStatusType().getValue());

        return targetSendResult.success();
    }

    private ReplaySendResult fromDubboResult(Map<?, ?> requestHeaders, String url, Object result,
                                             String traceId) {
        String body = encodeResponseAsString(result);
        HttpHeaders responseHeaders = new HttpHeaders();
        LOGGER.info("invoke result url:{}, request header:{}, response header:{}, body:{}", url, requestHeaders,
                responseHeaders, body);
        if (!isReplayRequest(requestHeaders)) {
            return ReplaySendResult.success(StringUtils.EMPTY, StringUtils.EMPTY, url);
        }
        if (responseHeaders.isEmpty()) {
            return ReplaySendResult.failed("dubbo replay error,review log find more details", url);
        }

        if (StringUtils.isEmpty(traceId)) {
            return ReplaySendResult.failed("Could not fetch replay result id from the headers of dubbo response", url);
        }

        return ReplaySendResult.success(traceId, StringUtils.EMPTY, url);
    }

    private ImmutablePair<String, String> getInterfaceNameAndMethod(String operationName) {
        if (StringUtils.isEmpty(operationName)) {
            return null;
        }
        int lastDotIndex = operationName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return null;
        }
        return ImmutablePair.of(operationName.substring(0, lastDotIndex), operationName.substring(lastDotIndex + 1));
    }

    private DubboParameters getDubboParameters(ReplayActionCaseItem caseItem) {
        String type = caseItem.getTargetRequest().getType();
        String body = caseItem.getTargetRequest().getBody();

        List<Object> parameters = new ArrayList<>();

        if (StringUtils.isNotEmpty(body)) {
            parameters.add(toParameter(body));
        }
        List<String> parameterTypes = new ArrayList<>();
        if (StringUtils.isNotEmpty(type)) {
            parameterTypes.add(type);
        }
        DubboParameters dubboParameters = new DubboParameters();
        dubboParameters.setParameterTypes(parameterTypes);
        dubboParameters.setParameters(parameters);
        return dubboParameters;
    }

    private Object toParameter(String body) {
        JSONObject object = tryParseJsonObject(body);
        if (object != null) {
            return object;
        }
        JSONArray array = tryParseJsonArray(body);
        if (array != null) {
            return array;
        }
        return body;
    }

    private JSONObject tryParseJsonObject(String body) {
        if (!body.startsWith(CommonConstant.JSON_START)) {
            return null;
        }
        try {
            return JSONObject.parseObject(body);
        } catch (Exception e) {
            return null;
        }
    }
    private JSONArray tryParseJsonArray(String body) {
        if (!body.startsWith(CommonConstant.JSON_ARRAY_START)) {
            return null;
        }
        try {
            return JSONArray.parseArray(body);
        } catch (Exception e) {
            return null;
        }
    }

    @Data
    private static class DubboParameters {
        private List<String> parameterTypes;
        private List<Object> parameters;
    }
}

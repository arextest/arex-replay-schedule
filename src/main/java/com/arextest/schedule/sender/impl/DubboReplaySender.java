package com.arextest.schedule.sender.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.sender.ReplaySendResult;
import com.arextest.schedule.sender.SenderParameters;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author b_yu
 * @since 2023/4/11
 */
@Slf4j
@Component
public class DubboReplaySender extends AbstractReplaySender {
    private static final String DUBBO_APP_NAME = "arex-replay";
    private static final String APPLICATION_JSON = "application/json";

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
        String exclusionOperationConfig = replayActionItem.getExclusionOperationConfig();
        if (StringUtils.isNotEmpty(exclusionOperationConfig)) {
            headers.put(CommonConstant.X_AREX_EXCLUSION_OPERATIONS, exclusionOperationConfig);
        }
        return doSend(caseItem, headers);
    }

    @Override
    public boolean send(ReplayActionCaseItem caseItem, Map<String, String> headers) {
        return this.send(caseItem);
    }

    @Override
    public ReplaySendResult send(SenderParameters senderParameters) {
        return null;
    }

    private boolean doSend(ReplayActionCaseItem caseItem, Map<String, String> headers) {
        ImmutablePair<String, String> interfaceNameAndMethod =
                getInterfaceNameAndMethod(caseItem.getParent().getOperationName());
        if (interfaceNameAndMethod == null) {
            return false;
        }

        ServiceInstance instanceRunner = selectLoadBalanceInstance(caseItem.getId(), caseItem.getParent().getTargetInstance());
        if (instanceRunner == null) {
            return false;
        }
        String url = instanceRunner.getUrl();
        RpcContext.getServiceContext().setAttachments(headers);
        GenericService genericService = getReferenceConfig(url, interfaceNameAndMethod.getLeft());
        if (genericService == null) {
            return false;
        }
        DubboParameters dubboParameters = getDubboParameters(caseItem);
        Object result = genericService.$invoke(interfaceNameAndMethod.getRight(),
                dubboParameters.getParameterTypes().toArray(new String[0]),
                dubboParameters.getParameters().toArray());
        ReplaySendResult targetSendResult = fromDubboResult(headers, url, result);
        caseItem.setSendErrorMessage(targetSendResult.getRemark());
        caseItem.setTargetResultId(targetSendResult.getTraceId());
        caseItem.setSendStatus(targetSendResult.getStatusType().getValue());

        return targetSendResult.success();
    }

    private ReplaySendResult fromDubboResult(Map<?, ?> requestHeaders, String url, Object result) {
        String body = encodeResponseAsString(result);
        HttpHeaders responseHeaders = new HttpHeaders();
        String traceId = null;
        Map<String, String> attachments = RpcContext.getServerContext().getAttachments();
        if (MapUtils.isNotEmpty(attachments)) {
            attachments.forEach(responseHeaders::add);
            traceId = attachments.get(CommonConstant.AREX_REPLAY_ID);
        }
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

    private GenericService getReferenceConfig(String url, String interfaceName) {
        try {
            ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
            reference.setApplication(new ApplicationConfig(DUBBO_APP_NAME));
            reference.setUrl(url);
            reference.setInterface(interfaceName);
            reference.setGeneric(true);
            return reference.get();
        } catch (Exception e) {
            LOGGER.error("Get dubbo reference config error", e);
        }
        return null;
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

package com.arextest.schedule.sender.impl;

import com.alibaba.dubbo.common.api.LsfApi;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.protocol.dubbo.DecodeableRpcResult;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.common.FileUtils;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.config.CustomProtocolConfig;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.sender.ReplaySendResult;
import com.arextest.schedule.sender.SenderParameters;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author b_yu
 * @since 2023/4/11
 */
@Slf4j
@Component
public class DubboReplaySender extends AbstractReplaySender {
    private static final String DUBBO_APP_NAME = "arex-replay";
    private static final String APPLICATION_JSON = "application/json";
    private static final String EXTENSION_FILE_PATH = "/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/META-INF/dubbo/com.alibaba.dubbo.rpc.Protocol";
    //    private static final String EXTENSION_FILE_PATH = "../webapps/arex_schedule_web_api_war/WEB-INF/classes/META-INF/dubbo/com.alibaba.dubbo.rpc.Protocol";
    private static final String EXTENSION_FILE_PATH_PROPERTY_KEY = "arex.custom.protocol.textPath";
    private static final String EXTENSION_JAR_PATH_PROPERTY_KEY_PATTERN = "arex.custom.protocol.jarPath.%s";
    private static final ConcurrentHashMap<String, CustomProtocolConfig> customProtocolMap = new ConcurrentHashMap<>();


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
        LsfApi.putTag(CommonConstant.AREX_RECORD_ID, caseItem.getRecordId());
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
        String protocol = instanceRunner.getProtocol();

        //custom dubbo protocol
        if (!StringUtils.equals(protocol, ServiceInstance.DUBBO_PROTOCOL)) {
            LOGGER.info("custom dubbo protocol:{}", protocol);
            //handleCustomProtocol(protocol);
        }

        RpcContext.getContext().setAttachments(headers);
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

    private void handleCustomProtocol(String protocol) {
        //existing protocol, skip
        if (protocol == null || customProtocolMap.containsKey(protocol)) {
            return;
        }
        //user appoint extension text file path
        String textFilePath = System.getProperty(EXTENSION_FILE_PATH_PROPERTY_KEY);
        FileUtils.copy(textFilePath, EXTENSION_FILE_PATH);

        List<String> protocolConfigs = FileUtils.readInLine(EXTENSION_FILE_PATH);
        LOGGER.info("procotol configs:{}", protocolConfigs);
        String jarPath = null;
        for(String line: protocolConfigs) {
            String name = null;
            int i = line.indexOf(61);
            if (i > 0) name = line.substring(0, i).trim();
            if (name != null && StringUtils.equals(protocol, name)) {
                //user appoint protocol jar file path
                jarPath = System.getProperty(String.format(EXTENSION_JAR_PATH_PROPERTY_KEY_PATTERN, name)) ;
                customProtocolMap.put(name, new CustomProtocolConfig(name, jarPath));
            }
        }
        if (jarPath != null) {
            FileUtils.loadJar(jarPath);
        }
    }

    private ReplaySendResult fromDubboResult(Map<?, ?> requestHeaders, String url, Object result) {
        String body = encodeResponseAsString(result);
        HttpHeaders responseHeaders = new HttpHeaders();
        String traceId = null;
        Map<String, String> attachments = RpcContext.getContext().getAttachments();
        if (MapUtils.isNotEmpty(attachments)) {
            attachments.forEach(responseHeaders::add);
            traceId = attachments.get(CommonConstant.AREX_REPLAY_ID);
        }
        if (traceId == null) traceId = DecodeableRpcResult.replayId;
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

package com.arextest.schedule.sender.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.schedule.DubboInvoker;
import com.arextest.schedule.ReplayInvokeResult;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.sender.ReplaySendResult;
import com.arextest.schedule.sender.SenderParameters;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.loader.WebappClassLoaderBase;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
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
    private static final String JAR_FILE_PATH = "D:\\Users\\yushuwang\\work\\lib\\dubboInvoker-1.0-SNAPSHOT.jar";
    private static final String ADD_URL_FUN_NAME = "addURL";
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

        ReplayInvokeResult replayInvokeResult = null;
        DubboParameters dubboParameters = getDubboParameters(caseItem);

        loadJar(JAR_FILE_PATH);

        ServiceLoader<DubboInvoker> loader = ServiceLoader.load(DubboInvoker.class);
        for (DubboInvoker invoker : loader) {
            if (invoker.getName().equalsIgnoreCase("default")) {
                replayInvokeResult = invoker.invoke(url, headers,
                        interfaceNameAndMethod.getLeft(), interfaceNameAndMethod.getRight(),
                        dubboParameters.parameterTypes, dubboParameters.getParameters());
            }
        }
//        RpcContext.getServiceContext().setAttachments(headers);
//        GenericService genericService = getReferenceConfig(url, interfaceNameAndMethod.getLeft());
//        if (genericService == null) {
//            return false;
//        }
//        DubboParameters dubboParameters = getDubboParameters(caseItem);
//         result = genericService.$invoke(interfaceNameAndMethod.getRight(),
//                dubboParameters.getParameterTypes().toArray(new String[0]),
//                dubboParameters.getParameters().toArray());
//        ReplaySendResult targetSendResult = fromDubboResult(headers, url, result);
        if (replayInvokeResult == null) {
            return false;
        }
        ReplaySendResult targetSendResult = fromDubboResult(headers, url,
                replayInvokeResult.getResult(), replayInvokeResult.getAttachments());
        caseItem.setSendErrorMessage(targetSendResult.getRemark());
        caseItem.setTargetResultId(targetSendResult.getTraceId());
        caseItem.setSendStatus(targetSendResult.getStatusType().getValue());

        return targetSendResult.success();
    }

    public static void loadJar(String jarPath) {
        File jarFile = new File(jarPath);
        Method method = null;
        try {
            method = WebappClassLoaderBase.class.getDeclaredMethod(ADD_URL_FUN_NAME, URL.class);
        } catch (NoSuchMethodException | SecurityException e1) {
            LOGGER.error("getDeclaredMethod failed, jarPath:{}, message:{}", jarPath, e1.getMessage());
        }
        boolean accessible = method.isAccessible();
        try {
            method.setAccessible(true);
            Class<?> clazz = DubboReplaySender.class;
            ClassLoader classLoader = clazz.getClassLoader();
            URL url = jarFile.toURI().toURL();
            method.invoke(classLoader, url);
        } catch (Exception e2) {
            LOGGER.error("addUrl failed, jarPath:{}, message:{}", jarPath, e2.getMessage());
        } finally {
            method.setAccessible(accessible);
        }
    }

    private ReplaySendResult fromDubboResult(Map<?, ?> requestHeaders, String url, Object result,
                                             Map<String, String> attachments) {
        String body = encodeResponseAsString(result);
        HttpHeaders responseHeaders = new HttpHeaders();
        String traceId = null;
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

//    private GenericService getReferenceConfig(String url, String interfaceName) {
//        try {
//            ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
//            reference.setApplication(new ApplicationConfig(DUBBO_APP_NAME));
//            reference.setUrl(url);
//            reference.setInterface(interfaceName);
//            reference.setGeneric(true);
//            return reference.get();
//        } catch (Exception e) {
//            LOGGER.error("Get dubbo reference config error", e);
//        }
//        return null;
//    }

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

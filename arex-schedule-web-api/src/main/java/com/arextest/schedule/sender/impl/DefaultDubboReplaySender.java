package com.arextest.schedule.sender.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.arextest.common.model.classloader.RemoteJarClassLoader;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.common.UrlUtil;
import com.arextest.schedule.extension.invoker.ReplayExtensionInvoker;
import com.arextest.schedule.extension.model.ReplayInvokeResult;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.model.invocation.DubboInvocation;
import com.arextest.schedule.sender.ReplaySendResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.http.HttpHeaders;

/**
 * @author b_yu
 * @since 2023/4/11
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultDubboReplaySender extends AbstractReplaySender {

  private static final String VERSION = "version";
  private static final String GROUP = "group";

  private final List<String> headerExcludes;
  private final List<ReplayExtensionInvoker> replayExtensionInvokers;
  private final RemoteJarClassLoader dubboInvokerLoader;

  @Override
  public boolean isSupported(String categoryType) {
    return MockCategoryType.DUBBO_PROVIDER.getName().equals(categoryType);
  }

  @Override
  public int getOrder() {
    return -1;
  }

  @Override
  public boolean send(ReplayActionCaseItem caseItem) {
    ClassLoader old = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(dubboInvokerLoader);
    try {
      before(caseItem);
      Map<String, String> headers = createHeaders(caseItem);
      return doSend(caseItem, headers);
    } finally {
      Thread.currentThread().setContextClassLoader(old);
    }
  }

  @Override
  public boolean send(ReplayActionCaseItem caseItem, Map<String, String> extraHeaders) {
    Map<String, String> headers = createHeaders(caseItem);
    headers.putAll(extraHeaders);
    return doSend(caseItem, headers);
  }

  DubboInvocation generateDubboInvocation(ReplayActionCaseItem caseItem,
      Map<String, String> headers) {

    // remove attachment excludes
    headerExcludes.forEach(headers::remove);

    ImmutablePair<String, String> interfaceNameAndMethod =
        getInterfaceNameAndMethod(caseItem.getParent().getOperationName());
    if (interfaceNameAndMethod == null) {
      LOGGER.error("getInterfaceNameAndMethod failed, caseItem:{}", caseItem);
      return null;
    }
    ServiceInstance instanceRunner = selectLoadBalanceInstance(caseItem.getId(),
        caseItem.getParent().getTargetInstance());
    if (instanceRunner == null) {
      LOGGER.error("selectLoadBalanceInstance failed, caseItem:{}", caseItem);
      return null;
    }
    String url = appendVersionAndGroup(instanceRunner.getUrl(), headers);

    DubboParameters dubboParameters = getDubboParameters(caseItem);
    return new DubboInvocation(url, headers, interfaceNameAndMethod.left,
        interfaceNameAndMethod.right,
        dubboParameters.getParameterTypes(), dubboParameters.getParameters());
  }

  private String appendVersionAndGroup(String url, Map<String, String> headers) {
    if(UrlUtil.getParamFromUrl(url, VERSION) == null && headers.containsKey(VERSION)) {
      url = UrlUtil.appendParamToUrl(url, VERSION, headers.get(VERSION));
    }
    if(UrlUtil.getParamFromUrl(url, GROUP) == null && headers.containsKey(GROUP)) {
      url = UrlUtil.appendParamToUrl(url, GROUP, headers.get(GROUP));
    }
    return url;
  }

  private boolean doSend(ReplayActionCaseItem caseItem, Map<String, String> headers) {

    ReplayInvokeResult replayInvokeResult = null;

    DubboInvocation dubboInvocation = generateDubboInvocation(caseItem, headers);
    LOGGER.info("dubboInvocation:{}", dubboInvocation);
    if (dubboInvocation == null) {
      return false;
    }
    if (CollectionUtils.isEmpty(replayExtensionInvokers)) {
      LOGGER.error("no invokers");
    }
    for (ReplayExtensionInvoker invoker : replayExtensionInvokers) {
      if (invoker.isSupported(caseItem.getCaseType())) {
        dubboInvocation.setInvoker(invoker);
        replayInvokeResult = invoker.invoke(dubboInvocation);
        break;
      }
    }
    if (replayInvokeResult == null) {
      LOGGER.error("replayInvokeResult is null, caseItem:{}", caseItem);
      return false;
    }

    if (replayInvokeResult.getException() != null) {
      LOGGER.error("dubbo invoke error msg:{}", replayInvokeResult.getErrorMsg(),
          replayInvokeResult.getException());
    }

    ReplaySendResult targetSendResult = fromDubboResult(headers, dubboInvocation.getUrl(),
        replayInvokeResult.getResult(), replayInvokeResult.getResponseProperties());
    caseItem.setSendErrorMessage(targetSendResult.getRemark());
    caseItem.setTargetResultId(targetSendResult.getTraceId());
    caseItem.setSendStatus(targetSendResult.getStatusType().getValue());

    return targetSendResult.success();
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
    LOGGER.info("invoke result url:{}, request header:{}, response header:{}, body:{}", url,
        requestHeaders,
        responseHeaders, body);
    if (!isReplayRequest(requestHeaders)) {
      return ReplaySendResult.success(StringUtils.EMPTY, StringUtils.EMPTY, url);
    }
    if (responseHeaders.isEmpty()) {
      return ReplaySendResult.failed("dubbo replay error,review log find more details", url);
    }

    if (StringUtils.isEmpty(traceId)) {
      return ReplaySendResult.failed(
          "Could not fetch replay result id from the headers of dubbo response", url);
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
    return ImmutablePair.of(operationName.substring(0, lastDotIndex),
        operationName.substring(lastDotIndex + 1));
  }

  private DubboParameters getDubboParameters(ReplayActionCaseItem caseItem) {
    String type = caseItem.getTargetRequest().getType();
    String body = caseItem.getTargetRequest().getBody();

    DubboParameters dubboParameters = new DubboParameters();
    dubboParameters.setParameterTypes(toParameterTypes(type));
    dubboParameters.setParameters(toParameters(body, type));
    return dubboParameters;
  }

  public static List<String> toParameterTypes(String type) {
    List<String> parameterTypes = new ArrayList<>();
    if (StringUtils.isNotEmpty(type)) {
      JSONArray array = null;
      if (type.startsWith(CommonConstant.JSON_ARRAY_START)) {
        array = tryParseJsonArray(type);
      }
      if (array == null) {
        parameterTypes.add(type);
      } else {
        parameterTypes.addAll(array.toJavaList(String.class));
      }
    }
    return parameterTypes;
  }

  public static List<Object> toParameters(String body, String type) {
    List<Object> parameters = new ArrayList<>();
    if (StringUtils.isNotEmpty(body) && StringUtils.isNotEmpty(type)) {
      JSONArray array = null;
      //type starts with "[", cuz single-object body could start with "["
      if (type.startsWith(CommonConstant.JSON_ARRAY_START)) {
        array = tryParseJsonArray(body);
      }
      if (array == null) {
        parameters.add(toParameter(body));
      } else {
        parameters.addAll(array);
      }
    }
    return parameters;
  }

  private static Object toParameter(String body) {
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

  private static JSONObject tryParseJsonObject(String body) {
    if (!body.startsWith(CommonConstant.JSON_START)) {
      return null;
    }
    try {
      return JSONObject.parseObject(body);
    } catch (Exception e) {
      return null;
    }
  }

  private static JSONArray tryParseJsonArray(String body) {
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

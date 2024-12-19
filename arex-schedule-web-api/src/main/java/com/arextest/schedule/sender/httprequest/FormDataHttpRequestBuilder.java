package com.arextest.schedule.sender.httprequest;

import com.arextest.schedule.model.sender.HttpSenderContent;
import com.arextest.schedule.sender.SenderParameters;
import com.arextest.schedule.utils.DecodeUtils;
import com.google.common.base.Strings;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Slf4j
@Component
@Order(1)
public class FormDataHttpRequestBuilder extends AbstractHttpRequestBuilder {

  private static final String CONTENT_TYPE = "multipart/form-data";

  @Override
  public boolean supportBuild(SenderParameters senderParameters) {
    if (senderParameters == null) {
      return false;
    }

    if (!shouldApplyHttpBody(HttpMethod.valueOf(senderParameters.getMethod()))) {
      return false;
    }

    String contentType = getContentType(senderParameters.getHeaders());
    if (contentType == null) {
      return false;
    }
    return contentType.contains(CONTENT_TYPE);
  }

  @Override
  public HttpSenderContent buildRequestContent(SenderParameters senderParameters) {

    HttpSenderContent httpSenderContent = new HttpSenderContent();

    // build url
    String requestUrl = contactUrl(senderParameters.getUrl(), senderParameters.getOperation());
    httpSenderContent.setRequestUrl(requestUrl);

    // build http method
    String method = senderParameters.getMethod();
    HttpMethod httpMethod = HttpMethod.valueOf(method);
    httpSenderContent.setHttpMethod(httpMethod);

    // build headers
    HttpHeaders httpHeaders = createRequestHeaders(senderParameters.getHeaders(),
        senderParameters.getFormat());
    httpHeaders.remove("content-length");

    // build http entity
    HttpEntity<?> httpEntity = null;
    String message = senderParameters.getMessage();

    Object res;
    Object decodeMessage = DecodeUtils.decode(message);
    if (decodeMessage instanceof byte[]) {
      String temp = new String((byte[]) decodeMessage, StandardCharsets.UTF_8);
      Map<String, List<String>> tempMap = splitQuery(temp);
      MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
      for (Map.Entry<String, List<String>> entry : tempMap.entrySet()) {
        String key = entry.getKey();
        List<String> values = entry.getValue();
        map.put(key, new ArrayList<>(values));
      }
      res = map;
    } else {
      res = decodeMessage;
    }

    httpEntity = new HttpEntity<>(res, httpHeaders);
    httpSenderContent.setResponseType(String.class);
    httpSenderContent.setHttpEntity(httpEntity);
    return httpSenderContent;
  }

  public Map<String, List<String>> splitQuery(String queryString) {
    if (Strings.isNullOrEmpty(queryString)) {
      return Collections.emptyMap();
    }
    return Arrays.stream(queryString.split("&"))
        .map(this::splitQueryParameter)
        .collect(Collectors.groupingBy(SimpleImmutableEntry::getKey, LinkedHashMap::new,
            Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
  }

  public SimpleImmutableEntry<String, String> splitQueryParameter(String it) {
    final int idx = it.indexOf("=");
    final String key = idx > 0 ? it.substring(0, idx) : it;
    final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;

    String decodeKey = StringUtils.EMPTY;
    String decodeValue = null;
    try {
      decodeKey = URLDecoder.decode(key, String.valueOf(StandardCharsets.UTF_8));
      decodeValue =
          value != null ? URLDecoder.decode(value, String.valueOf(StandardCharsets.UTF_8)) : null;
    } catch (UnsupportedEncodingException e) {
      LOGGER.error("Failed to decode key: {}", key, e);
    }
    return new SimpleImmutableEntry<>(decodeKey, decodeValue);
  }

}

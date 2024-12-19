package com.arextest.schedule.sender.httprequest;

import com.arextest.schedule.model.sender.HttpSenderContent;
import com.arextest.schedule.sender.SenderParameters;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

public abstract class AbstractHttpRequestBuilder {

  public abstract boolean supportBuild(SenderParameters senderParameters);

  public abstract HttpSenderContent buildRequestContent(SenderParameters senderParameters);


  protected HttpHeaders createRequestHeaders(Map<String, String> sourceHeaders, String format) {
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
    httpHeaders.remove(HttpHeaders.CONTENT_LENGTH);
    return httpHeaders;
  }

  protected String contactUrl(String baseUrl, String operation) {
    String result = null;
    if (StringUtils.endsWith(baseUrl, "/") || StringUtils.startsWith(operation, "/")) {
      result = baseUrl + operation;
    } else {
      result = baseUrl + "/" + operation;
    }
    return result;
  }


  boolean shouldApplyHttpBody(HttpMethod httpMethod) {
    return httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT
        || httpMethod == HttpMethod.PATCH ||
        httpMethod == HttpMethod.DELETE;
  }

  String getContentType(Map<String, String> sourceHeaders) {
    if (MapUtils.isEmpty(sourceHeaders)) {
      return null;
    }

    for (Map.Entry<String, String> entry : sourceHeaders.entrySet()) {
      String key = entry.getKey();
      if (!StringUtils.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE, key)) {
        continue;
      }
      return entry.getValue();
    }
    return null;
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


}

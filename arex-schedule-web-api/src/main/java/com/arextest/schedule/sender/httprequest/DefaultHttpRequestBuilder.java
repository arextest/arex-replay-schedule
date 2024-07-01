package com.arextest.schedule.sender.httprequest;

import com.arextest.schedule.model.sender.HttpSenderContent;
import com.arextest.schedule.sender.SenderParameters;
import com.arextest.schedule.utils.DecodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Slf4j
@Order()
@Component
public class DefaultHttpRequestBuilder extends AbstractHttpRequestBuilder {

  @Override
  public boolean supportBuild(SenderParameters senderParameters) {
    return true;
  }

  @Override
  public HttpSenderContent buildRequestContent(SenderParameters senderParameters) {
    HttpSenderContent httpSenderContent = new HttpSenderContent();

    // build url
    String requestUrl =
        contactUrl(senderParameters.getUrl(), senderParameters.getOperation());
    httpSenderContent.setRequestUrl(requestUrl);

    // build http method
    String method = senderParameters.getMethod();
    HttpMethod httpMethod = HttpMethod.resolve(method);
    httpSenderContent.setHttpMethod(httpMethod);

    // build headers
    HttpHeaders httpHeaders = createRequestHeaders(senderParameters.getHeaders(),
        senderParameters.getFormat());

    Class<?> responseType = String.class;
    String requestMessage = senderParameters.getMessage();
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
    httpSenderContent.setResponseType(responseType);
    httpSenderContent.setHttpEntity(httpEntity);
    return httpSenderContent;
  }
}

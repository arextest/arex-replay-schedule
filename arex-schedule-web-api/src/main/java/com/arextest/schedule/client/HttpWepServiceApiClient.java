package com.arextest.schedule.client;

import static com.arextest.schedule.common.CommonConstant.URL;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import com.arextest.schedule.utils.SSLUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * @author jmo
 * @since 2021/9/15
 */
@Component
@Slf4j
public final class HttpWepServiceApiClient {

  private RestTemplate restTemplate;
  private RetryTemplate retryTemplate;
  @Resource
  private ZstdJacksonMessageConverter zstdJacksonMessageConverter;
  @Resource
  private ObjectMapper objectMapper;
  @Value("${arex.connect.time.out}")
  private int connectTimeOut;
  @Value("${arex.read.time.out}")
  private int readTimeOut;
  @Value("${arex.retry.max.attempts}")
  private int maxAttempts;
  @Value("${arex.retry.back.off.period}")
  private int backOffPeriod;
  @Value("${arex.client.https.cert.disable:#{false}}")
  private boolean disableCertCheck;

  @Autowired(required = false)
  private List<ClientHttpRequestInterceptor> clientHttpRequestInterceptors;
  private static final String ZSTD_JSON_CONTENT_TYPE = "application/zstd-json;charset=UTF-8";

  @PostConstruct
  private void initTemplate() {
    initRestTemplate();
    initRetryTemplate();
    if (disableCertCheck) {
      SSLUtils.disableSSLVerification();
    }
  }

  private void initRestTemplate() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeOut);
    requestFactory.setReadTimeout(readTimeOut);
    final int initialCapacity = 10;
    List<HttpMessageConverter<?>> httpMessageConverterList = new ArrayList<>(initialCapacity);
    httpMessageConverterList.add(zstdJacksonMessageConverter);
    httpMessageConverterList.add(new ByteArrayHttpMessageConverter());
    httpMessageConverterList.add(new StringHttpMessageConverter());
    httpMessageConverterList.add(new ResourceHttpMessageConverter());
    httpMessageConverterList.add(new SourceHttpMessageConverter<>());
    httpMessageConverterList.add(new AllEncompassingFormHttpMessageConverter());

    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(
        objectMapper);
    converter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));
    httpMessageConverterList.add(converter);
    this.restTemplate = new RestTemplate(httpMessageConverterList);
    this.restTemplate.setRequestFactory(requestFactory);
    if (CollectionUtils.isNotEmpty(clientHttpRequestInterceptors)) {
      restTemplate.setInterceptors(clientHttpRequestInterceptors);
    }
  }

  private void initRetryTemplate() {
    retryTemplate = new RetryTemplate();

    // Create an Retry Policy that will retry on specific exceptions, up to maxAttempts times
    setRetryPolicy();
    // Create an FixedBackOffPolicy that will set the retry interval
    setBackOffPolicy();
    // Add retry listener
    registerRetryListener();
  }

  private void setBackOffPolicy() {
    FixedBackOffPolicy backoffPolicy = new FixedBackOffPolicy();
    backoffPolicy.setBackOffPeriod(backOffPeriod);
    retryTemplate.setBackOffPolicy(backoffPolicy);
  }

  private void setRetryPolicy() {
    // Create a SimpleRetryPolicy that will retry up to maxAttempts times
    SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(maxAttempts);

    // Create an ExceptionClassifierRetryPolicy that will retry on specific exceptions, up to maxAttempts times
    ExceptionClassifierRetryPolicy exceptionClassifierRetryPolicy = new ExceptionClassifierRetryPolicy();

    Map<Class<? extends Throwable>, RetryPolicy> policyMap = Maps.newHashMapWithExpectedSize(3);
    policyMap.put(SocketException.class, simpleRetryPolicy);
    policyMap.put(SocketTimeoutException.class, simpleRetryPolicy);
    policyMap.put(ResourceAccessException.class, simpleRetryPolicy);
    exceptionClassifierRetryPolicy.setPolicyMap(policyMap);

    retryTemplate.setRetryPolicy(exceptionClassifierRetryPolicy);
  }

  private void registerRetryListener() {
    retryTemplate.registerListener(new RetryListener() {
      @Override
      public <T, E extends Throwable> boolean open(RetryContext context,
          RetryCallback<T, E> callback) {
        return true;
      }

      @Override
      public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
          Throwable throwable) {
      }

      @Override
      public <T, E extends Throwable> void onError(RetryContext context,
          RetryCallback<T, E> callback, Throwable throwable) {
        LOGGER.warn("Retry url: {}, count: {}, error message: {}", context.getAttribute(URL),
            context.getRetryCount(),
            throwable.getMessage());
      }
    });
  }

  public <TResponse> TResponse get(String url, Map<String, ?> urlVariables,
      Class<TResponse> responseType) {
    try {
      return restTemplate.getForObject(url, responseType, urlVariables);
    } catch (Exception e) {
      return null;
    }
  }

  public <TResponse> ResponseEntity<TResponse> get(String url, Map<String, ?> urlVariables,
      ParameterizedTypeReference<TResponse> responseType) {
    try {
      return restTemplate.exchange(url, HttpMethod.GET, null, responseType, urlVariables);
    } catch (Exception e) {
      return null;
    }
  }

  public <TResponse> ResponseEntity<TResponse> retryGet(String url, Map<String, ?> urlVariables,
      ParameterizedTypeReference<TResponse> responseType) {
    try {
      return retryTemplate.execute(retryCallback -> {
        retryCallback.setAttribute(URL, url);
        return restTemplate.exchange(url, HttpMethod.GET, null, responseType, urlVariables);
      });
    } catch (Exception e) {
      return null;
    }
  }

  public <TResponse> TResponse get(String url, Map<String, ?> urlVariables,
      MultiValueMap<String, String> headers, Class<TResponse> responseType) {
    try {
      HttpEntity<?> request = new HttpEntity<>(headers);
      return restTemplate.exchange(url, HttpMethod.GET, request, responseType, urlVariables)
          .getBody();
    } catch (Exception e) {
      return null;
    }
  }

  public <TRequest, TResponse> TResponse jsonPost(String url, TRequest request,
      Class<TResponse> responseType) {
    try {
      return restTemplate.postForObject(url, wrapJsonContentType(request), responseType);
    } catch (Exception e) {
      return null;
    }
  }

  public <TRequest, TResponse> TResponse jsonPost(String url, TRequest request,
      Class<TResponse> responseType,
      Map<String, String> headers) {
    try {

      return restTemplate.postForObject(url, wrapJsonContentType(request, headers), responseType);
    } catch (Exception e) {
      return null;
    }
  }

  public <TRequest, TResponse> TResponse retryJsonPost(String url, TRequest request,
      Class<TResponse> responseType) {
    try {
      return retryTemplate.execute(retryCallback -> {
        retryCallback.setAttribute(URL, url);
        return restTemplate.postForObject(url, wrapJsonContentType(request), responseType);
      });
    } catch (Exception e) {
      return null;
    }
  }

  public <TRequest, TResponse> TResponse retryJsonPost(String url, TRequest request,
      Class<TResponse> responseType, Map<String, String> headers) {
    try {
      return retryTemplate.execute(retryCallback -> {
        retryCallback.setAttribute(URL, url);
        return restTemplate.postForObject(url, wrapJsonContentType(request, headers), responseType);
      });
    } catch (Exception e) {
      return null;
    }
  }

  public <TRequest, TResponse> ResponseEntity<TResponse> retryJsonPost(String url, TRequest request,
      ParameterizedTypeReference<TResponse> responseType) {
    try {
      return retryTemplate.execute(retryCallback -> {
        retryCallback.setAttribute(URL, url);
        return restTemplate.exchange(url, HttpMethod.POST, wrapJsonContentType(request),
            responseType);
      });
    } catch (Exception e) {
      return null;
    }
  }

  public <TRequest, TResponse> TResponse retryZstdJsonPost(String url, TRequest request,
      Class<TResponse> responseType) {
    Map<String, String> headers = Maps.newHashMapWithExpectedSize(1);
    headers.put(HttpHeaders.CONTENT_TYPE, ZSTD_JSON_CONTENT_TYPE);

    try {
      return retryTemplate.execute(retryCallback -> {
        retryCallback.setAttribute(URL, url);
        return restTemplate.postForObject(url, wrapJsonContentType(request, headers), responseType);
      });
    } catch (Exception e) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private <TRequest> HttpEntity<TRequest> wrapJsonContentType(TRequest request) {
    HttpEntity<TRequest> httpJsonEntity;
    if (request instanceof HttpEntity) {
      httpJsonEntity = (HttpEntity<TRequest>) request;
    } else {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      httpJsonEntity = new HttpEntity<>(request, headers);
    }
    return httpJsonEntity;
  }

  @SuppressWarnings("unchecked")
  private <TRequest> HttpEntity<TRequest> wrapJsonContentType(TRequest request,
      Map<String, String> extraHeaders) {
    HttpEntity<TRequest> httpJsonEntity;
    if (request instanceof HttpEntity) {
      httpJsonEntity = (HttpEntity<TRequest>) request;
    } else {
      HttpHeaders headers = new HttpHeaders();
      if (!extraHeaders.containsKey(CONTENT_TYPE)) {
        headers.setContentType(MediaType.APPLICATION_JSON);
      }
      headers.setAll(extraHeaders);
      httpJsonEntity = new HttpEntity<>(request, headers);
    }
    return httpJsonEntity;
  }

  /**
   * When restTemplate sends a replay request, it needs to pass the URI object to avoid the url
   * encode parameter parsing exception.
   */
  public <TResponse> ResponseEntity<TResponse> exchange(String url, HttpMethod method,
      HttpEntity<?> requestEntity,
      Class<TResponse> responseType) throws RestClientException {
    return restTemplate.exchange(URI.create(url), method, requestEntity, responseType);
  }

  public <TRequest, TResponse> ResponseEntity<TResponse> jsonPostWithThrow(String url,
      HttpEntity<TRequest> request,
      Class<TResponse> responseType) throws RestClientException {
    return restTemplate.postForEntity(url, wrapJsonContentType(request), responseType);

  }

  public <TRequest, TResponse> ResponseEntity<TResponse> retryJsonPostWithThrow(String url,
      HttpEntity<TRequest> request,
      Class<TResponse> responseType) throws RestClientException {
    return retryTemplate.execute(retryCallback -> {
      retryCallback.setAttribute(URL, url);
      return restTemplate.postForEntity(url, wrapJsonContentType(request), responseType);
    });
  }
}
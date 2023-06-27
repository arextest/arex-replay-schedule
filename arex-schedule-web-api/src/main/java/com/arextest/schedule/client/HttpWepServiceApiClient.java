package com.arextest.schedule.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author jmo
 * @since 2021/9/15
 */
@Component
@Slf4j
public final class HttpWepServiceApiClient {
    private RestTemplate restTemplate;
    @Resource
    private ZstdJacksonMessageConverter zstdJacksonMessageConverter;
    @Resource
    private ObjectMapper objectMapper;
    @Value("${arex.connect.time.out}")
    private int connectTimeOut;
    @Value("${arex.read.time.out}")
    private int readTimeOut;


    @PostConstruct
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

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));
        httpMessageConverterList.add(converter);
        this.restTemplate = new RestTemplate(httpMessageConverterList);
        this.restTemplate.setRequestFactory(requestFactory);
    }

    public <TResponse> TResponse get(String url, Map<String, ?> urlVariables, Class<TResponse> responseType) {
        try {
            return restTemplate.getForObject(url, responseType, urlVariables);
        } catch (Throwable throwable) {
            LOGGER.error("http get url: {} ,error: {} , urlVariables: {}", url, throwable.getMessage(), urlVariables,
                    throwable);
        }
        return null;
    }

    public <TResponse> ResponseEntity<TResponse> get(String url, Map<String, ?> urlVariables, ParameterizedTypeReference<TResponse> responseType) {
        try {
            return restTemplate.exchange(url, HttpMethod.GET, null, responseType, urlVariables);
        } catch (Throwable throwable) {
            LOGGER.error("http get url: {} ,error: {} , urlVariables: {}", url, throwable.getMessage(), urlVariables,
                    throwable);
        }
        return null;
    }

    public <TResponse> TResponse get(String url, Map<String, ?> urlVariables,
                                     MultiValueMap<String, String> headers, Class<TResponse> responseType) {
        try {
            HttpEntity<?> request = new HttpEntity<>(headers);
            return restTemplate.exchange(url, HttpMethod.GET, request, responseType, urlVariables).getBody();
        } catch (Throwable throwable) {
            LOGGER.error("http get url: {} ,error: {} , urlVariables: {} ,headers: {}", url, throwable.getMessage(),
                    urlVariables, headers,
                    throwable);
        }
        return null;
    }

    public <TRequest, TResponse> TResponse jsonPost(String url, TRequest request, Class<TResponse> responseType) {
        try {
            return restTemplate.postForObject(url, wrapJsonContentType(request), responseType);
        } catch (Throwable throwable) {
            try {
                LOGGER.error("http post url: {} ,error: {} ,request: {}", url, throwable.getMessage(),
                        objectMapper.writeValueAsString(request), throwable);
            } catch (JsonProcessingException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return null;
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

    public <TResponse> ResponseEntity<TResponse> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
                                                          Class<TResponse> responseType) throws RestClientException {
        return restTemplate.exchange(url, method, requestEntity, responseType);
    }

    public <TRequest, TResponse> ResponseEntity<TResponse> jsonPostWithThrow(String url, HttpEntity<TRequest> request,
                                                                             Class<TResponse> responseType) throws RestClientException {
        return restTemplate.postForEntity(url, wrapJsonContentType(request), responseType);

    }
}
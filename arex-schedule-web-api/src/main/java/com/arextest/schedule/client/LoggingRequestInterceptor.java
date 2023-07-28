package com.arextest.schedule.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Base64;

/**
 * supply a interceptor to log request info
 * <p>
 * created by xinyuan_wang on 2023/7/26
 */
@Slf4j
public class LoggingRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        String requestBody = null;
        if (body != null && body.length > 0) {
            requestBody = Base64.getEncoder().encodeToString(body);
        }

        ClientHttpResponse response;
        try {
            response = execution.execute(request, body);
        } catch (IOException ex) {
            LOGGER.warn(String.format("Failed to send %s request to %s, body: %s", request.getMethod(), request.getURI(), requestBody), ex);
            throw ex;
        }

        HttpStatus statusCode = response.getStatusCode();

        // Log the failed request if the response status code is 4xx or 5xx
        if (statusCode.is4xxClientError() || statusCode.is5xxServerError()) {
            LOGGER.warn("Failed to send {} request to {}, requestBody: {}, statusCode: {}", request.getMethod(), request.getURI(), requestBody, statusCode);
        }

        return response;
    }

}

package com.arextest.schedule.model.sender;

import lombok.Data;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

@Data
public class HttpSenderContent {

  private String requestUrl;

  private HttpMethod httpMethod;

  private HttpEntity<?> httpEntity;

  private Class<?> responseType;
}

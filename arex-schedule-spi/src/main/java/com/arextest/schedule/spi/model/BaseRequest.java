package com.arextest.schedule.spi.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class BaseRequest extends ReplayInvokeRequest {
    private String url;
    private Map<String, String> headers;
}

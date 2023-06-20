package com.arextest.schedule.spi.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class DubboRequest extends BaseRequest {
    private String interfaceName;
    private String methodName;
    private List<String> parameterTypes;
    private List<Object> parameters;
}

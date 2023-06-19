package com.arextest.schedule.model;

import lombok.Data;

import java.util.Set;

/**
 * @author jmo
 * @since 2021/9/22
 */
@Data
public class AppServiceOperationDescriptor {
    private String id;
    private AppServiceDescriptor parent;
    private String operationName;
    private int status;
    private String operationType;
    private Set<String> operationTypes;
}
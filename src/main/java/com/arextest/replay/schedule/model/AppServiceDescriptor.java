package com.arextest.replay.schedule.model;

import com.arextest.replay.schedule.model.deploy.ServiceInstance;
import lombok.Data;

import java.util.List;

/**
 * @author jmo
 * @since 2021/9/22
 */
@Data
public class AppServiceDescriptor {
    private long id;
    private String serviceKey;
    private String serviceName;
    private String appId;
    private List<AppServiceOperationDescriptor> operationList;
    private List<ServiceInstance> targetActiveInstanceList;
    private List<ServiceInstance> sourceActiveInstanceList;
}

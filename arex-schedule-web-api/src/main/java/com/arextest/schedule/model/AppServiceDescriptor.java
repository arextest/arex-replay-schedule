package com.arextest.schedule.model;

import com.arextest.schedule.model.deploy.ServiceInstance;
import java.util.List;
import lombok.Data;

/**
 * @author jmo
 * @since 2021/9/22
 */
@Data
public class AppServiceDescriptor {

  private String id;
  private String serviceKey;
  private String serviceName;
  private String appId;
  private List<AppServiceOperationDescriptor> operationList;
  private List<ServiceInstance> targetActiveInstanceList;
  private List<ServiceInstance> sourceActiveInstanceList;
}
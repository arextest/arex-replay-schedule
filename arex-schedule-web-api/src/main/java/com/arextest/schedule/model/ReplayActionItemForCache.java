package com.arextest.schedule.model;

import com.arextest.schedule.model.deploy.ServiceInstance;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wildeslam.
 * @create 2023/11/23 16:14
 */
@Data
@EqualsAndHashCode(of = {"id"})
public class ReplayActionItemForCache {
  private String id;
  private String operationId;
  private String operationName;
  private String planId;
  private String appId;
  private String serviceKey;
  private List<ServiceInstance> targetInstance;
  private String exclusionOperationConfig;
}

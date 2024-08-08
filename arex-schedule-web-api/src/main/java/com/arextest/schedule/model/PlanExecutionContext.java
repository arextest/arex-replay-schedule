package com.arextest.schedule.model;

import com.arextest.schedule.model.deploy.ServiceInstance;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Map;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.mongodb.core.query.Criteria;

/**
 * Created by Qzmo on 2023/5/15
 */
@Data
public class PlanExecutionContext<T> {

  private static final String CONTEXT_PREFIX = "batch-";

  private static final String NO_CONTEXT_SUFFIX = "no-config";

  @JsonIgnore
  private ReplayPlan plan;

  private String contextName;
  private ExecutionContextActionType actionType = ExecutionContextActionType.NORMAL;

  @JsonIgnore
  private ExecutionStatus executionStatus;

  // extra condition provide for case selecting before send
  @JsonIgnore
  private List<Criteria> contextCaseQuery;

  @JsonIgnore
  private T dependencies;

  private List<String> warmupFailedServerUrls;

  private Map<ServiceInstance,List<ServiceInstance>> bindInstanceMap;

  private Map<String, ReplayActionItem> currentCachedActionItemsMap;

  private List<ServiceInstance> currentTargetInstances;

  private List<ServiceInstance> currentSourceInstances;

  public static String buildContextName(String identifier) {
    String suffix = identifier == null ? NO_CONTEXT_SUFFIX : identifier;
    return CONTEXT_PREFIX + suffix;
  }

  public void removeTargetInstance(ServiceInstance instance) {
    if (instance == null || CollectionUtils.isEmpty(currentTargetInstances)) {
      return;
    }
    synchronized (this) {
      currentTargetInstances.remove(instance);
    }
  }
}

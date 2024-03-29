package com.arextest.schedule.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import lombok.Data;
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

  public static String buildContextName(String identifier) {
    String suffix = identifier == null ? NO_CONTEXT_SUFFIX : identifier;
    return CONTEXT_PREFIX + suffix;
  }
}

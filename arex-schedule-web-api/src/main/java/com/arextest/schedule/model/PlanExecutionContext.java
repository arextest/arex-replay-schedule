package com.arextest.schedule.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Qzmo on 2023/5/15
 */
@Data
public class PlanExecutionContext<T> {
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

    @JsonIgnore
    private Set<ReplayActionItem> actionItemSet = new HashSet<>();
}

package com.arextest.schedule.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

/**
 * Created by Qzmo on 2023/5/15
 */
@Data
public class PlanExecutionContext<T> {
    private String contextName;

    @JsonIgnore
    private ExecutionStatus executionStatus;

    // extra condition provide for case selecting before send
    @JsonIgnore
    private List<Criteria> contextCaseQuery;

    @JsonIgnore
    private T dependencies;
}

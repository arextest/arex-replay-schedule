package com.arextest.schedule.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Map;

/**
 * Created by Qzmo on 2023/5/15
 */
@Data
public class PlanExecutionContext<T> {
    private String contextName;

    // extra condition provide for case selecting before send
    @JsonIgnore
    private Query contextCaceQuery;

    @JsonIgnore
    private T dependencies;
}

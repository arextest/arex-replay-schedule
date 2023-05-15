package com.arextest.schedule.model;

import lombok.Data;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Map;

/**
 * Created by Qzmo on 2023/5/15
 */
@Data
public class PlanExecutionContext {
    // extra condition provide for case selecting before send
    private Query contextCaceQuery;

    private Map<String, String> dependencies;
}

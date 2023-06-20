package com.arextest.schedule.model.report;

import com.arextest.diff.model.log.LogEntity;
import lombok.Data;

/**
 * Created by rchen9 on 2023/4/12.
 */
@Data
public class QueryLogEntityResponseType {
    private int diffResultCode;
    private LogEntity logEntity;
}

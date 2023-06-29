package com.arextest.schedule.model.report;

import com.arextest.diff.model.log.LogEntity;
import lombok.Data;

/**
 * Created by qzmo on 2023/6/27.
 */
@Data
public class QueryLogEntityResponseType {
    private int diffResultCode;
    private LogEntity logEntity;
}
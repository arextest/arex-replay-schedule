package com.arextest.schedule.model.plan;

import lombok.Data;

import java.util.List;

/**
 * Created by wang_yc on 2021/9/15
 */
@Data
public class OperationCaseInfo {
    private String operationId;
    private List<String> replayIdList;
}
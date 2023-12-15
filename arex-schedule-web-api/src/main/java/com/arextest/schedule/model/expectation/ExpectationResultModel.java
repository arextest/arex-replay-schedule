package com.arextest.schedule.model.expectation;

import lombok.Data;

/**
 * @since 2023/12/17
 */
@Data
public class ExpectationResultModel {
    private String caseId;
    private String category;
    private String operation;
    private String path;
    private String message;
    private boolean result;
    private String assertionText;
    private Long dataChangeCreateTime;
}

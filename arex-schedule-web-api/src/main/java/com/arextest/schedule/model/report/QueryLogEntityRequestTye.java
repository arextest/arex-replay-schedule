package com.arextest.schedule.model.report;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Created by qzmo on 2023/06/27.
 */
@Data
public class QueryLogEntityRequestTye {
    @NotBlank(message = "id cannot be empty")
    private String compareResultId;
    private int logIndex;
}
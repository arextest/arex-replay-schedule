package com.arextest.schedule.model.report;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Created by rchen9 on 2023/4/12.
 */
@Data
public class QueryLogEntityRequestTye {
    @NotBlank(message = "id cannot be empty")
    private String compareResultId;
    private int logIndex;
}

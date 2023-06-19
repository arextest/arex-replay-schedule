package com.arextest.schedule.model.bizlog;

import lombok.Data;

import java.util.List;
import java.util.Optional;

/**
 * Created by Qzmo on 2023/6/8
 */
@Data
public class ReplayBizLogQueryCondition {
    private Integer pageNum;
    private Integer pageSize;
    private List<Integer> levels;
    private List<Integer> types;
    private List<String> actionItems;
    private Boolean resumedExecution;

    public void validate() {
        this.pageNum = Optional.ofNullable(this.getPageNum()).filter(i -> i > 0).orElse(1);
        this.pageSize = Optional.ofNullable(this.getPageSize()).filter(i -> i > 0).orElse(100);
    }
}

package com.arextest.schedule.model.plan;

import lombok.Data;

/**
 * @author wildeslam.
 * @create 2023/7/24 17:13
 */
@Data
public class StageBaseInfo {
    /**
     * @see PlanStageEnum
     */
    private int stageType;
    private String stageName;
    private String msg;
    /**
     * @see StageStatusEnum
     */
    private int stageStatus;
    private Long startTime;
    private Long endTime;
}

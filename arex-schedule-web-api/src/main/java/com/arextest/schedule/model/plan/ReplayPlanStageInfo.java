package com.arextest.schedule.model.plan;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wildeslam.
 * @create 2023/7/24 17:14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReplayPlanStageInfo extends StageBaseInfo {
    private List<StageBaseInfo> subStageInfoList;


    public static ReplayPlanStageInfo initStage(PlanStageEnum planStageEnum) {
        ReplayPlanStageInfo stageInfo = new ReplayPlanStageInfo();
        stageInfo.setStageType(planStageEnum.getCode());
        stageInfo.setStageName(planStageEnum.getDesc());
        stageInfo.setStageStatus(StageStatusEnum.PENDING.getCode());
        return stageInfo;
    }
}

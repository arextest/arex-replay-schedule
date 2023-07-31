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
    private List<ReplayPlanStageInfo> subStageInfoList;
}

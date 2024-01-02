package com.arextest.schedule.utils;

import com.arextest.schedule.model.plan.PlanStageEnum;
import com.arextest.schedule.model.plan.ReplayPlanStageInfo;
import com.arextest.schedule.model.plan.StageStatusEnum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;

/**
 * @author wildeslam.
 * @create 2023/7/26 10:51
 */
public class StageUtils {

  public static final List<PlanStageEnum> INITIAL_STAGES = Arrays.asList(PlanStageEnum.INIT,
      PlanStageEnum.PRE_LOAD,
      PlanStageEnum.RUN, PlanStageEnum.FINISH);
  public static final String MSG_FORMAT = "%s is %s";
  public static final String RUNNING_FORMAT = "start at %s, end at %s";
  public static final String RUN_MSG_FORMAT_SINGLE = "Total batches:%d, %d batch has been executed.";
  public static final String RUN_MSG_FORMAT = "Total batches:%d, %d batches have been executed.";


  public static ReplayPlanStageInfo initEmptyStage(PlanStageEnum planStageEnum) {
    ReplayPlanStageInfo stageInfo = new ReplayPlanStageInfo();
    stageInfo.setStageType(planStageEnum.getCode());
    stageInfo.setStageName(planStageEnum.name());
    stageInfo.setStageStatus(StageStatusEnum.PENDING.getCode());
    return stageInfo;
  }

  public static List<ReplayPlanStageInfo> initPlanStageList(List<PlanStageEnum> planStageEnumList) {
    List<ReplayPlanStageInfo> replayPlanStageList = new ArrayList<>(planStageEnumList.size());
    for (PlanStageEnum planStageEnum : planStageEnumList) {
      ReplayPlanStageInfo replayPlanStageInfo = initEmptyStage(planStageEnum);
      if (CollectionUtils.isNotEmpty(planStageEnum.getSubStageList())) {
        replayPlanStageInfo.setSubStageInfoList(
            initPlanStageList(planStageEnum.getSubStageList().stream().map(PlanStageEnum::of)
                .collect(Collectors.toList()))
        );
      }
      replayPlanStageList.add(replayPlanStageInfo);
    }
    return replayPlanStageList;
  }

  public static void resetStageStatus(ReplayPlanStageInfo replayPlanStageInfo) {
    replayPlanStageInfo.setStageStatus(StageStatusEnum.PENDING.getCode());
    replayPlanStageInfo.setStartTime(null);
    replayPlanStageInfo.setEndTime(null);
    replayPlanStageInfo.setMsg(null);
    if (CollectionUtils.isNotEmpty(replayPlanStageInfo.getSubStageInfoList())) {
      replayPlanStageInfo.getSubStageInfoList().forEach(StageUtils::resetStageStatus);
    }
  }
}

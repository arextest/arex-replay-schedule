package com.arextest.schedule.utils;

import com.arextest.schedule.model.plan.PlanStageEnum;
import com.arextest.schedule.model.plan.ReplayPlanStageInfo;
import com.arextest.schedule.model.plan.StageBaseInfo;
import com.arextest.schedule.model.plan.StageStatusEnum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;

/**
 * @author wildeslam.
 * @create 2023/7/26 10:51
 */
public class StageUtils {
    public static final List<PlanStageEnum> INITIAL_STAGES = Arrays.asList(PlanStageEnum.INIT, PlanStageEnum.PRE_LOAD,
        PlanStageEnum.RUN, PlanStageEnum.FINISH);
    private static final String MSG_FORMAT = "%s is %s";
    public static final String RUN_MSG_FORMAT = "Total batches:%d, %d has been executed.";

    private static ReplayPlanStageInfo initEmptyStage(PlanStageEnum planStageEnum) {
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
                    initPlanStageList(planStageEnum.getSubStageList().stream().map(PlanStageEnum::of).collect(Collectors.toList()))
                );
            }
            replayPlanStageList.add(replayPlanStageInfo);
        }
        return replayPlanStageList;
    }

    public static void updateStage(PlanStageEnum stageType, Long startTime, Long endTime, StageStatusEnum stageStatus,
                                   String msg, List<ReplayPlanStageInfo> stageInfoList) {
        StageBaseInfo stageBaseInfo = null;
        boolean isSubStage = stageType.getSubStageList() == null;
        if (isSubStage) {
            PlanStageEnum parentStage = PlanStageEnum.of(stageType.getParentStage());
            List<StageBaseInfo> subStageInfoList = stageInfoList.stream()
                .map(ReplayPlanStageInfo::getSubStageInfoList)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
            for (StageBaseInfo subStage : subStageInfoList) {
                if (stageType.getCode() == subStage.getStageType()) {
                    stageBaseInfo = subStage;
                }
            }

            // when first subStage starts, start its parent stage
            if (stageStatus == StageStatusEnum.ONGOING && parentStage.getSubStageList().get(0) == stageType.getCode()) {
                updateStage(parentStage, startTime, endTime, stageStatus, null, stageInfoList);
            }
            // when last subStage successes, end its parent stage
            else if (stageStatus == StageStatusEnum.SUCCEEDED &&
                parentStage.getSubStageList().get(parentStage.getSubStageList().size() - 1) == stageType.getCode()) {
                updateStage(parentStage, startTime, endTime, stageStatus, null, stageInfoList);
            }
            // when any subStage fails, fail its parent stage
            else if (stageStatus == StageStatusEnum.FAILED ) {
                updateStage(parentStage, startTime, endTime, stageStatus, null, stageInfoList);
            }
        } else {
            for (StageBaseInfo stage : stageInfoList) {
                if (stageType.getCode() == stage.getStageType()) {
                    stageBaseInfo = stage;
                }
            }
        }
        if (stageBaseInfo != null) {
            if (msg == null) {
                msg = String.format(MSG_FORMAT, stageType.name(), stageStatus);
            }
            stageBaseInfo.setStageType(stageType.getCode());
            stageBaseInfo.setStageName(stageType.name());
            if (stageStatus != null) {
                stageBaseInfo.setStageStatus(stageStatus.getCode());
            }
            stageBaseInfo.setMsg(msg);
            if (startTime != null) {
                stageBaseInfo.setStartTime(startTime);
            }
            if (endTime != null) {
                stageBaseInfo.setEndTime(endTime);
            }
        }
    }

    public static void insertStage(List<ReplayPlanStageInfo> stageInfoList, PlanStageEnum planStageEnum) {

    }
}

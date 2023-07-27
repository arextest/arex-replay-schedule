package com.arextest.schedule.model.plan;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * @author wildeslam.
 * @create 2023/7/24 16:48
 */
public enum PlanStageEnum {
    UNKNOWN(0, null, null),
    WAIT(10, Arrays.asList(101, 102), null),
    PACKAGE_IMAGE(101, null, 10),
    DEPLOY_IMAGE(102, null, 10),

    INIT(20, Arrays.asList(201, 202, 203), null),
    BUILD_PLAN(201, null, 20),
    SAVE_PLAN(202, null, 20),
    INIT_REPORT(203, null, 20),

    PRE_LOAD(30, Arrays.asList(301, 302, 303), null),
    LOADING_CONFIG(301, null, 30),
    LOADING_CASE(302, null, 30),
    BUILD_CONTEXT(303, null, 30),

    RUN(40, Collections.EMPTY_LIST, null),
    RE_RUN(41, Collections.EMPTY_LIST, null),

    CANCEL(50, Collections.EMPTY_LIST, null),

    FINISH(60, Collections.EMPTY_LIST, null);


    @Getter
    private final int code;

    // MainStage's subStageList should not be null but empty list even if it hasn't subStages.
    @Getter
    private final List<Integer> subStageList;
    @Getter
    private final Integer parentStage;

    PlanStageEnum(int code, List<Integer> subStageList, Integer parentStage) {
        this.code = code;
        this.subStageList = subStageList;
        this.parentStage = parentStage;
    }

    private final static Map<Integer, PlanStageEnum> CODE_VALUE_MAP = Arrays.stream(PlanStageEnum.values())
        .collect(Collectors.toMap(PlanStageEnum::getCode, Function.identity()));

    public static PlanStageEnum of(int code) {
        return CODE_VALUE_MAP.getOrDefault(code, UNKNOWN);
    }
}

package com.arextest.schedule.model.plan;

import lombok.Getter;

/**
 * @author wildeslam.
 * @create 2023/7/24 16:48
 */
public enum PlanStageEnum {
    INIT(20, "初始化", true),
    BUILD_PLAN(201, "创建任务", true),
    SAVE_PLAN(202, "保存任务", true),

    PRE_LOAD(30, "预加载", true),
    LOADING_CONFIG(301, "加载配置", true),
    LOADING_CASE(302, "加载用例", true),
    BUILD_CONTEXT(303, "创建上下文", true),

    RUN(40, "运行", true),
    RE_RUN(41, "重跑", false),

    CANCEL(50, "取消", false),

    FINISH(60, "完成", true);


    @Getter
    private final int code;
    @Getter
    private final String desc;
    @Getter
    private final boolean isMainProcess;

    PlanStageEnum(int code, String desc, boolean isMainProcess) {
        this.code = code;
        this.desc = desc;
        this.isMainProcess = isMainProcess;
    }

    public static int MAIN_PROCESS_STAGE_NUM = 4;
}

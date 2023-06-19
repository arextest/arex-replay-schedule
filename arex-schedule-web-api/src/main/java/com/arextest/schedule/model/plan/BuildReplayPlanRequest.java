package com.arextest.schedule.model.plan;

import com.arextest.schedule.model.CaseSourceEnvType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Created by wang_yc on 2021/9/15
 */
@Data
public class BuildReplayPlanRequest {

    private String appId;

    private String planName;
    /**
     * @see BuildReplayPlanType
     */
    private int replayPlanType;
    private String operator;

    private String sourceEnv;

    private String targetEnv;

    /**
     * @see CaseSourceEnvType
     */
    private int caseSourceType;

    private int caseCountLimit;

    /**
     * yyyy-MM-dd HH:mm:ss
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date caseSourceFrom;
    /**
     * yyyy-MM-dd HH:mm:ss
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date caseSourceTo;

    private List<OperationCaseInfo> operationCaseInfoList;   //replay_plan_type=1or2的时候 来这里获取 需要回放的接口或case


}
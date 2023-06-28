package com.arextest.schedule.model.dao.mongodb;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by rchen9 on 2022/8/18.
 */
@Data
@NoArgsConstructor
@Document("ReplayPlanItem")
public class ReplayPlanItemCollection extends ModelBase {
    @NonNull
    private String operationId;
    @NonNull
    private String planId;
    @NonNull
    private int replayStatus;
    private Date replayBeginTime;
    private Date replayCaseLoadedTime;
    private Date replayFinishTime;
    @NonNull
    private int replayCaseCount;
    @NonNull
    private String appId;
}
package com.arextest.schedule.model.dao.mongodb;

import com.arextest.schedule.model.plan.ReplayPlanStageInfo;
import java.util.Date;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by rchen9 on 2022/8/18.
 */
@Data
@NoArgsConstructor
@Document("ReplayPlan")
public class ReplayPlanCollection extends ModelBase {

  @NonNull
  private String appId;
  @NonNull
  private String planName;
  @NonNull
  private String sourceEnv;
  @NonNull
  private String targetEnv;
  @NonNull
  private String sourceHost;
  @NonNull
  private String targetHost;
  @NonNull
  private String targetImageId;
  @NonNull
  private String targetImageName;
  private Date caseSourceFrom;
  private Date caseSourceTo;
  @NonNull
  private Date planCreateTime;
  private Date planFinishTime;
  @NonNull
  private String operator;
  private String arexCordVersion;
  private String arexExtVersion;
  private String caseRecordVersion;
  private int caseTotalCount;
  private int caseSourceType;
  private int replayPlanType;
  private int caseCountLimit;

  private List<ReplayPlanStageInfo> replayPlanStageList;
}
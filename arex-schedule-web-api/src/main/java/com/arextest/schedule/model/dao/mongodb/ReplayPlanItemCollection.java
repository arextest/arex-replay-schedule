package com.arextest.schedule.model.dao.mongodb;

import java.util.Date;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by rchen9 on 2022/8/18.
 */
@Data
@NoArgsConstructor
@Document("ReplayPlanItem")
@FieldNameConstants
public class ReplayPlanItemCollection extends ModelBase {

  @NonNull
  private String appId;
  @NonNull
  private String operationId;
  @NonNull
  private String planId;
  @NonNull
  private int replayCaseCount;

  private Map<String, Integer> noiseFinishedContexts;

  @NonNull
  private int replayStatus;
  private Date replayBeginTime;
  private Date replayFinishTime;
}
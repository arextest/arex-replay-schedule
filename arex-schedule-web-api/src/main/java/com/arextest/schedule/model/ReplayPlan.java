package com.arextest.schedule.model;

import com.arextest.schedule.common.RateLimiterFactory;
import com.arextest.schedule.model.bizlog.BizLog;
import com.arextest.schedule.model.dao.mongodb.ReplayPlanCollection;
import com.arextest.schedule.model.plan.BuildReplayPlanType;
import com.arextest.schedule.model.plan.ReplayPlanStageInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jmo
 * @see ReplayPlanCollection
 * @since 2021/9/15
 */
@Data
@ToString(of = {"id", "appId", "sourceEnv", "sourceHost", "targetEnv", "targetHost"})
@EqualsAndHashCode(of = {"id"})
@Slf4j
public class ReplayPlan {

  private String id;
  private String appId;
  private Integer replaySendMaxQps;
  @JsonIgnore
  private String planName;
  // @JsonIgnore
  private String sourceEnv;
  // @JsonIgnore
  private String targetEnv;
  @JsonIgnore
  private String sourceHost;
  @JsonIgnore
  private String targetHost;
  @JsonIgnore
  private String targetImageId;
  @JsonIgnore
  private String targetImageName;
  @JsonIgnore
  private Date caseSourceFrom;
  @JsonIgnore
  private Date caseSourceTo;
  @JsonIgnore
  private Date planCreateTime;
  @JsonIgnore
  private Date planFinishTime;
  @JsonIgnore
  private String operator;
  @JsonIgnore
  private String arexCordVersion;
  @JsonIgnore
  private String arexExtVersion;
  @JsonIgnore
  private String caseRecordVersion;
  private int caseTotalCount;
  private int caseRerunCount;
  /**
   * see {@link CaseSourceEnvType}
   */
  @JsonIgnore
  private int caseSourceType;
  /**
   * @see BuildReplayPlanType
   */
  @JsonIgnore
  private int replayPlanType;
  @JsonIgnore
  private List<ReplayActionItem> replayActionItemList;
  @JsonIgnore
  private List<PlanExecutionContext<?>> executionContexts;
  @JsonIgnore
  private String appName;
  @JsonIgnore
  private int caseCountLimit;
  @JsonIgnore
  private String errorMessage;
  private transient long planCreateMillis;

  private boolean resumed;

  @JsonIgnore
  private ExecutionStatus planStatus;

  @JsonIgnore
  private long lastLogTime = System.currentTimeMillis();

  @JsonIgnore
  private BlockingQueue<BizLog> bizLogs = new LinkedBlockingQueue<>();
  @JsonIgnore
  private List<ScheduledFuture<?>> monitorFutures;
  @JsonIgnore
  private Map<String, ReplayActionItem> actionItemMap = new HashMap<>();
  private List<ReplayPlanStageInfo> replayPlanStageList;
  @JsonIgnore
  private long lastUpdateTime = System.currentTimeMillis();
  private boolean reRun;

  @JsonIgnore
  private Map<String, String> caseTags;
  private boolean initReportItem;
  @JsonIgnore
  private RateLimiterFactory rateLimiterFactory;

  public void enqueueBizLog(BizLog log) {
    this.bizLogs.add(log);
  }

  public void buildActionItemMap() {
    this.getReplayActionItemList().forEach(
        replayActionItem -> this.actionItemMap.put(replayActionItem.getId(), replayActionItem));
  }
}
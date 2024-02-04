package com.arextest.schedule.model.dao.mongodb;

import java.util.Date;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;


@Data
@Document(collection = "ReplayCompareResult")
public class ReplayCompareResultCollection extends ModelBase {

  private String planId;
  private String planItemId;
  private String caseId;

  private String operationId;

  private String serviceName;

  private String categoryName;

  private String operationName;

  private String replayId;

  private String recordId;


  private long recordTime;

  private long replayTime;

  private String instanceId;

  private String baseMsg;

  private String testMsg;

  private String logs;

  private Boolean ignore;

  private int diffResultCode;

  private Date dataCreateTime;

  private ReplayCompareMsgInfoCollection msgInfo;
}
package com.arextest.schedule.model.dao.mongodb;

import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by qzmo on 2023/5/31.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Document(ReplayBizLogCollection.COLLECTION_NAME)
public class ReplayBizLogCollection extends ModelBase {

  public static final String COLLECTION_NAME = "ReplayBizLog";
  private Date date;
  private int level;
  private String message;
  private int logType;

  private String planId;
  private boolean resumedExecution;
  private String contextName;

  private String contextIdentifier;
  private String caseItemId;
  private String actionItemId;
  private String operationName;

  private String exception;
  private String request;
  private String response;
  private String traceId;
  private String extra;
  private Boolean reRun;
}
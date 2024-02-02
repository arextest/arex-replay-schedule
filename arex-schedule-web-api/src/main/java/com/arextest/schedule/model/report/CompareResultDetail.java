package com.arextest.schedule.model.report;

import com.arextest.diff.model.log.NodeEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import lombok.Data;

@Data
public class CompareResultDetail {

  private String id;
  private String categoryName;
  private String operationName;

  private List<LogInfo> logInfos;
  private String baseMsg;
  private String testMsg;

  private Integer diffResultCode;
  private String exceptionMsg;

  private Boolean ignore;

  @Data
  public static class LogInfo {

    private int count;
    private List<NodeEntity> nodePath;
    @JsonIgnore
    private int unmatchedType;
    private int logIndex;
  }
}
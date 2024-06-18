package com.arextest.schedule.model.collection;

import com.arextest.diff.model.log.LogEntity;
import com.arextest.diff.model.log.NodeEntity;
import java.util.List;
import lombok.Data;

@Data
public class CompareMsgResponseType {

  private List<LogDetail> logDetails;
  private String baseMsg;
  private String testMsg;

  private Integer diffResultCode;
  private String exceptionMsg;


  @Data
  public static class LogDetail {

    private int count;
    private List<NodeEntity> nodePath;
    private LogEntity logEntity;
  }

}

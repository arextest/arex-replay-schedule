package com.arextest.schedule.model.collection;

import lombok.Data;

@Data
public class CompareMsgRequestType {

  private String baseMsg;

  private String testMsg;

  private String appId;

  private String operationName;

}

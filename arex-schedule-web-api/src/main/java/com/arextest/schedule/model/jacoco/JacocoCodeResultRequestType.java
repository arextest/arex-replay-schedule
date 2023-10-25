package com.arextest.schedule.model.jacoco;

import lombok.Data;

/**
 * Created by wang_yc on 2021/8/10
 */
@Data
public class JacocoCodeResultRequestType {

  private String recordId;
  private String jacocoExeId;
  private String jacocoExeStatus;
}
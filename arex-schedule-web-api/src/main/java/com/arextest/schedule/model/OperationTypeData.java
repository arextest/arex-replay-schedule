package com.arextest.schedule.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xinyuan_wang
 * @since 2024/03/04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationTypeData {

  /**
   * The earliest time of the saved case.
   * This field is required when evoking a task
   */
  private long lastRecordTime;
  /**
   * The number of corresponding matching cases that have been saved.
   * This field is required when evoking a task
   */
  private long totalLoadedCount;
  /**
   * eg. SOAProvider, DubboProvider
   */
  private String operationType;

  public OperationTypeData(String operationType) {
    this.operationType = operationType;
  }

}
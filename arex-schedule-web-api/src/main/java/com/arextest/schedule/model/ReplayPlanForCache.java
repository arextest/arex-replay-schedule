package com.arextest.schedule.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wildeslam.
 * @create 2024/5/17 15:01
 */
@Data
@EqualsAndHashCode(of = {"id"})
public class ReplayPlanForCache {
  private String id;
  private boolean rerun;
  private int caseRerunCount;
}

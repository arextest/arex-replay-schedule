package com.arextest.schedule.eventBus;

import lombok.Data;

/**
 * @author wildeslam.
 * @create 2023/12/11 16:14
 */
@Data
public class PlanAutoRerunEvent {
  private String planId;
}

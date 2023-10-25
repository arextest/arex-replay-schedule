package com.arextest.schedule.model.noiseidentify;

import java.util.Map;
import lombok.Data;

/**
 * Created by coryhh on 2023/10/17.
 */
@Data
public class ReplayNoiseDto {

  private String planId;

  private String planItemId;

  private String categoryName;

  private String operationId;

  private String operationName;

  // k: path v: data
  private Map<String, ReplayNoiseItemDto> mayIgnoreItems;

  private Map<String, ReplayNoiseItemDto> mayDisorderItems;

}

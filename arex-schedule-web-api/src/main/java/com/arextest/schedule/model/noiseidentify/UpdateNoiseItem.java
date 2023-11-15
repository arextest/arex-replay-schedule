package com.arextest.schedule.model.noiseidentify;

import java.util.Map;
import lombok.Data;

@Data
public class UpdateNoiseItem {

  private Map<String, Object> queryFields;

  private Map<String, Object> updateFields;

}

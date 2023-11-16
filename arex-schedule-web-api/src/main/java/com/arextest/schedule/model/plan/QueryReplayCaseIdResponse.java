package com.arextest.schedule.model.plan;

import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * @author wildeslam.
 * @create 2023/11/15 17:26
 */
@Data
public class QueryReplayCaseIdResponse {
  private Map<String, List<String>> batchCaseIdsMap;

  private Map<String, String> batchWarmUpCaseIdMap;

}

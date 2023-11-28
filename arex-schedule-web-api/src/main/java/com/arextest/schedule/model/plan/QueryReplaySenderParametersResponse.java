package com.arextest.schedule.model.plan;

import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.sender.ReplaySenderParameters;
import java.util.Map;
import lombok.Data;

/**
 * @author wildeslam.
 * @create 2023/11/15 17:37
 */
@Data
public class QueryReplaySenderParametersResponse {
  private Map<String, ReplaySenderParameters> replaySenderParametersMap;
}

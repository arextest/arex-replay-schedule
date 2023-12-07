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

  /**
   * value: compress from ReplaySenderParameters by ZSTD.
   * @see  ReplaySenderParameters
   */
  private Map<String, String> replaySenderParametersMap;
}

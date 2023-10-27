package com.arextest.schedule.model.bizlog;

import com.arextest.schedule.model.dao.mongodb.ReplayBizLogCollection;
import java.util.Collections;
import java.util.List;
import lombok.Data;

/**
 * Created by Qzmo on 2023/6/8
 */
@Data
public class QueryReplayBizLogsResponse {

  private String planId = "";
  private Long total = 0L;
  private List<ReplayBizLogCollection> logs = Collections.emptyList();
}

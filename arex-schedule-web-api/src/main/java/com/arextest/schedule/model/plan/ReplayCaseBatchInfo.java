package com.arextest.schedule.model.plan;

import java.util.Set;
import lombok.Data;

/**
 * @author wildeslam.
 * @create 2023/12/7 15:12
 */
@Data
public class ReplayCaseBatchInfo {

  private Set<String> caseIds;
  private String warmUpId;
}

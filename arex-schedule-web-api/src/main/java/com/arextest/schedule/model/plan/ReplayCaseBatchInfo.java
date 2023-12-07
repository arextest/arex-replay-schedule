package com.arextest.schedule.model.plan;

import java.util.List;
import lombok.Data;

/**
 * @author wildeslam.
 * @create 2023/12/7 15:12
 */
@Data
public class ReplayCaseBatchInfo {
  private List<String> caseIds;
  private String warmUpId;
}

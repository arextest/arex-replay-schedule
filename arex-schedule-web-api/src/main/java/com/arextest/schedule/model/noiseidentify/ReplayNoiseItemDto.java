package com.arextest.schedule.model.noiseidentify;

import com.arextest.diff.model.log.NodeEntity;
import com.arextest.schedule.model.ReplayCompareResult;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by coryhh on 2023/10/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplayNoiseItemDto {

  private List<NodeEntity> nodePath;

  private String compareResultId;
  private ReplayCompareResult compareResult;
  private List<Integer> logIndexes;

  // errors belonging to this path, and the number of errors
  private Map<String, Integer> subPaths;

  // wrong number of mockers
  private int caseCount;

  public int getPathCount() {
    return subPaths.size();
  }

  private Integer status;
}
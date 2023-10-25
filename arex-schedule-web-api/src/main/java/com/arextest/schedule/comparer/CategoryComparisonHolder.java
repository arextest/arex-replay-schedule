package com.arextest.schedule.comparer;

import java.util.List;
import lombok.Data;

/**
 * @author jmo
 * @since 2022/12/5
 */
@Data
public class CategoryComparisonHolder {

  private String categoryName;
  private List<CompareItem> record;
  private List<CompareItem> replayResult;
}
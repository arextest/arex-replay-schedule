package com.arextest.schedule.comparer;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author jmo
 * @since 2022/12/5
 */
@Data
public class CategoryComparisonHolder {

  private String categoryName;
  /**
   * Need to match the comparison relationship
   */
  private List<CompareItem> record;
  private List<CompareItem> replayResult;

  private Boolean needMatch;
  /**
   * Not need to match the comparison relationship
   */
  private CompareResultItem compareResultItem;

  @Data
  @AllArgsConstructor
  public static class CompareResultItem {
    CompareItem recordItem;
    CompareItem replayItem;
  }
}
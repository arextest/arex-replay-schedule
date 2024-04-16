package com.arextest.schedule.comparer;

import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayCompareResult;
import java.util.List;

/**
 * @author jmo
 * @since 2021/9/16
 */
public interface ReplayResultComparer {

  boolean compare(ReplayActionCaseItem caseItem, boolean useReplayId);

  /**
   * call by com.arextest.schedule.comparer.ReplayResultComparer#compare
   * (com.arextest.schedule.model.ReplayActionCaseItem,boolean) to do content compare,
   *
   * @param caseItem       case item
   * @param waitCompareMap the content to compare
   * @return
   */
  List<ReplayCompareResult> doContentCompare(ReplayActionCaseItem caseItem,
      List<CategoryComparisonHolder> waitCompareMap);
}
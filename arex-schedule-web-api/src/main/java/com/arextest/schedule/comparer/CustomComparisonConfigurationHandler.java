package com.arextest.schedule.comparer;

import com.arextest.diff.model.CompareOptions;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.config.ComparisonInterfaceConfig;
import com.arextest.schedule.model.config.ReplayComparisonConfig;

/**
 * @Author wang_yc
 * @Date 2023/5/25 22:29
 */
public interface CustomComparisonConfigurationHandler {

  void build(ReplayComparisonConfig config, ReplayActionItem actionItem);

  /**
   * Pick config for quickCompare. QuickCompare is invoked during the normal send->compare
   * procedure.
   *
   * @see com.arextest.schedule.comparer.impl.DefaultReplayResultComparer
   * @see com.arextest.diff.sdk.CompareSDK#quickCompare(String, String, CompareOptions)
   */
  ReplayComparisonConfig pickConfig(
      ComparisonInterfaceConfig operationConfig,
      CompareItem compareItem,
      String category);

  /**
   * Pick config for compare. Full compare is invoked when users want to view detail of the diff on
   * UI.
   *
   * @see com.arextest.schedule.service.report.QueryReplayMsgService#queryDiffMsgById(String)
   * @see com.arextest.diff.sdk.CompareSDK#compare(String, String, CompareOptions)
   */
  ReplayComparisonConfig pickConfig(
      ComparisonInterfaceConfig operationConfig,
      String category,
      String operationName);

  CompareOptions buildSkdOption(String category, ReplayComparisonConfig compareConfig);
}

package com.arextest.schedule.comparer;

import com.arextest.diff.model.CompareOptions;
import com.arextest.schedule.model.config.ComparisonGlobalConfig;
import com.arextest.schedule.model.config.ComparisonInterfaceConfig;
import com.arextest.schedule.model.config.ReplayComparisonConfig;

/**
 * Created by Qzmo on 2023/8/2
 */
public interface CompareConfigPicker {
    /**
     * Pick config for quickCompare.
     * QuickCompare is invoked during the normal send->compare procedure.
     *
     * @see com.arextest.schedule.comparer.impl.DefaultReplayResultComparer
     * @see com.arextest.diff.sdk.CompareSDK#quickCompare(String, String, CompareOptions)
     */
    ReplayComparisonConfig pickConfig(ComparisonGlobalConfig globalConfig,
                                      ComparisonInterfaceConfig operationConfig,
                                      CompareItem compareItem,
                                      String category);

    /**
     * Pick config for compare.
     * Full compare is invoked when users want to view detail of the diff on UI.
     *
     * @see com.arextest.schedule.service.report.QueryReplayMsgService#queryDiffMsgById(String)
     * @see com.arextest.diff.sdk.CompareSDK#compare(String, String, CompareOptions)
     */
    ReplayComparisonConfig pickConfig(ComparisonGlobalConfig globalConfig,
                                      ComparisonInterfaceConfig operationConfig,
                                      String category,
                                      String operationName);
}

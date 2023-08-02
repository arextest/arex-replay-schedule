package com.arextest.schedule.comparer;

import com.arextest.schedule.model.config.ComparisonGlobalConfig;
import com.arextest.schedule.model.config.ComparisonInterfaceConfig;
import com.arextest.schedule.model.config.ReplayComparisonConfig;

/**
 * Created by Qzmo on 2023/8/2
 */
public interface CompareConfigPicker {
    /**
     * Pick the
     */
    ReplayComparisonConfig pickConfig(ComparisonGlobalConfig comparisonGlobalConfig,
                                      ComparisonInterfaceConfig operationConfig,
                                      CompareItem compareItem,
                                      String category);

    ReplayComparisonConfig pickConfig(ComparisonGlobalConfig globalConfig,
                                      ComparisonInterfaceConfig operationConfig,
                                      String category,
                                      String operationName);
}

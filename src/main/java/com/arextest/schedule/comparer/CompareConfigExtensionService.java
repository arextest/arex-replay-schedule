package com.arextest.schedule.comparer;

import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.config.ReplayComparisonConfig;

/**
 * @Author wang_yc
 * @Date 2023/5/25 22:29
 */
public interface CompareConfigExtensionService {

    void build(ReplayComparisonConfig config, ReplayActionItem actionItem);
}

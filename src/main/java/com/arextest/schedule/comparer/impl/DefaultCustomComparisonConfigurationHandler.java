package com.arextest.schedule.comparer.impl;

import com.arextest.schedule.comparer.CustomComparisonConfigurationHandler;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.config.ReplayComparisonConfig;

/**
 * @Author wang_yc
 * @Date 2023/5/25 22:31
 */
public class DefaultCustomComparisonConfigurationHandler implements CustomComparisonConfigurationHandler {

    @Override
    public void build(ReplayComparisonConfig config, ReplayActionItem actionItem) {
        // if you want to add custom comparison configuration, you can do it here.
    }
}

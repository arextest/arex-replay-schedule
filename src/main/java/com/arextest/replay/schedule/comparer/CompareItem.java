package com.arextest.replay.schedule.comparer;

/**
 * @author jmo
 * @since 2021/11/23
 */
public interface CompareItem {
    String getCompareContent();

    String getCompareOperation();

    String getCompareService();
}

package com.arextest.schedule.comparer;

/**
 * @author jmo
 * @since 2021/11/23
 */
public interface CompareItem {

  String getCompareContent();

  String getCompareOperation();

  String getCompareService();

  String getCompareKey();

  String getOperationType();

  long getCreateTime();

  boolean isEntryPointCategory();
}
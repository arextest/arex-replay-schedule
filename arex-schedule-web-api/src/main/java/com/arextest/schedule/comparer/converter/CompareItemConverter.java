package com.arextest.schedule.comparer.converter;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.replay.CompareRelationResult;
import com.arextest.schedule.comparer.CompareItem;

public interface CompareItemConverter {
  String DEFAULT_CATEGORY_NAME = "default";

  default String getCategoryName() {
    return DEFAULT_CATEGORY_NAME;
  }

  /**
   * Convert mocker to compare item, The agent handles the compatibility needs before the match
   * @param mocker
   * @return
   */
  CompareItem convert(AREXMocker mocker);

  /**
   * Convert relation result to compare item
   * @param relationResult
   * @param recordCompareItem
   * @return
   */
  CompareItem convert(CompareRelationResult relationResult, boolean recordCompareItem);

}

package com.arextest.schedule.comparer.converter.impl;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.replay.CompareRelationResult;
import com.arextest.schedule.comparer.CompareItem;
import com.arextest.schedule.comparer.converter.CompareItemConverter;
import com.arextest.schedule.comparer.impl.PrepareCompareItemBuilder.CompareItemImpl;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author xinyuan_wang.
 * @create 2024/9/2 16:46
 */
@Slf4j
@Component
public class DefaultCompareItemConverterImpl implements CompareItemConverter {

  @Override
  public CompareItem convert(AREXMocker mocker) {
    if (mocker == null || mocker.getCategoryType() == null) {
      return null;
    }

    MockCategoryType categoryType = mocker.getCategoryType();
    String operationKey = mocker.getOperationName();
    long createTime = mocker.getCreationTime();
    String body;
    String compareKey = mocker.getId();
    boolean entryPointCategory = false;
    if (categoryType.isEntryPoint()) {
      body = Objects.isNull(mocker.getTargetResponse()) ? null
          : mocker.getTargetResponse().getBody();
      compareKey = null;
      entryPointCategory = true;
    } else {
      body = Objects.isNull(mocker.getTargetRequest()) ? null
          : mocker.getTargetRequest().getBody();
    }
    return new CompareItemImpl(operationKey, body, compareKey, createTime, entryPointCategory);
  }

  @Override
  public CompareItem convert(CompareRelationResult relationResult, boolean recordCompareItem) {
    if (relationResult == null) {
      return null;
    }

    return new CompareItemImpl(relationResult, recordCompareItem);
  }

}

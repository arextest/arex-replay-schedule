package com.arextest.schedule.comparer.converter.impl;

import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.model.replay.CompareRelationResult;
import com.arextest.schedule.comparer.CompareItem;
import com.arextest.schedule.comparer.converter.CompareItemConverter;
import com.arextest.schedule.comparer.impl.PrepareCompareItemBuilder.CompareItemImpl;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author xinyuan_wang.
 * @create 2024/9/2 16:46
 */
@Slf4j
@Component
public class RedisCompareItemConverterImpl implements CompareItemConverter {

  @Override
  public String getCategoryName() {
    return MockCategoryType.REDIS.getName();
  }

  @Override
  public CompareItem convert(AREXMocker mocker) {
    if (mocker == null || mocker.getCategoryType() == null) {
      return null;
    }

    MockCategoryType categoryType = mocker.getCategoryType();
    String operationKey = getOperationName(mocker.getTargetRequest(), mocker.getOperationName());
    String compareMessage = Objects.isNull(mocker.getTargetRequest()) ? null
        : mocker.getTargetRequest().getBody();

    return new CompareItemImpl(operationKey, compareMessage, mocker.getId(),
        mocker.getCreationTime(), categoryType.isEntryPoint());
  }
  @Override
  public CompareItem convert(CompareRelationResult relationResult, boolean recordCompareItem) {
    if (relationResult == null || relationResult.getCategoryType() == null) {
      return null;
    }

    String message = recordCompareItem ? relationResult.getRecordMessage() : relationResult.getReplayMessage();
    long createTime = recordCompareItem ? relationResult.getRecordTime() : relationResult.getReplayTime();
    return new CompareItemImpl(relationResult.getOperationName(), message, null,
            createTime, relationResult.getCategoryType().isEntryPoint());
  }

  private String getOperationName(Target target, String operationName) {
    if (target == null) {
      return operationName;
    }

    String compareOperationName = target.attributeAsString(MockAttributeNames.CLUSTER_NAME);
    if (StringUtils.isNotBlank(compareOperationName)) {
      return compareOperationName;
    }
    return operationName;
  }
}

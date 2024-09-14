package com.arextest.schedule.comparer.converter.impl;

import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.model.replay.CompareRelationResult;
import com.arextest.schedule.common.JsonUtils;
import com.arextest.schedule.comparer.CompareItem;
import com.arextest.schedule.comparer.converter.CompareItemConverter;
import com.arextest.schedule.comparer.impl.PrepareCompareItemBuilder.CompareItemImpl;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author xinyuan_wang.
 * @create 2024/9/2 16:46
 */
@Slf4j
@Component
public class DatabaseCompareItemConverterImpl implements CompareItemConverter {

  @Override
  public String getCategoryName() {
    return MockCategoryType.DATABASE.getName();
  }

  @Override
  public CompareItem convert(AREXMocker mocker) {
    if (mocker == null || mocker.getCategoryType() == null) {
      return null;
    }

    String operationKey = getOperationName(mocker.getTargetRequest(), mocker.getOperationName());
    return new CompareItemImpl(operationKey, buildAttributes(mocker.getTargetRequest()).toString(),
        mocker.getId(), mocker.getCreationTime(), mocker.getCategoryType().isEntryPoint());
  }

  @Override
  public CompareItem convert(CompareRelationResult relationResult, boolean recordCompareItem) {
    if (relationResult == null || relationResult.getCategoryType() == null) {
      return null;
    }

    String message = recordCompareItem ? relationResult.getRecordMessage() : relationResult.getReplayMessage();
    long createTime = recordCompareItem ? relationResult.getRecordTime() : relationResult.getReplayTime();
    return new CompareItemImpl(relationResult.getOperationName(), processMessage(message), null,
            createTime, relationResult.getCategoryType().isEntryPoint());
  }

  private String processMessage(String message) {
    if (StringUtils.isBlank(message)) {
      return message;
    }

    Target target = JsonUtils.jsonStringToObject(message, Target.class);
    if (target != null) {
      message = buildAttributes(target).toString();
    }
    return message;
  }

  private ObjectNode buildAttributes(Target target) {
    ObjectNode obj = JsonNodeFactory.instance.objectNode();
    if (target == null) {
        return obj;
    }
    Map<String, Object> attributes = target.getAttributes();
    if (attributes != null) {
        for (Entry<String, Object> entry : attributes.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                obj.put(entry.getKey(), (String) value);
            } else {
                obj.putPOJO(entry.getKey(), value);
            }
        }
    }
    if (StringUtils.isNotEmpty(target.getBody())) {
        obj.put("body", target.getBody());
    }
    return obj;
  }

  private String getOperationName(Target target, String operationName) {
    // The "@" in the operationName of DATABASE indicates that the SQL statement has been parsed and returned directly.
    String compareOperationName = StringUtils.contains(operationName, "@") ? operationName
      : target.attributeAsString(MockAttributeNames.DB_NAME);
    if (StringUtils.isNotBlank(compareOperationName)) {
      return compareOperationName;
    }
    return operationName;
  }
}

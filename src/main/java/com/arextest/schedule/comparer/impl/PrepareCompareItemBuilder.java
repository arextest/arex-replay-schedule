package com.arextest.schedule.comparer.impl;

import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.schedule.comparer.CompareItem;
import com.arextest.schedule.comparer.sqlparse.SqlParseHandler;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * @author jmo
 * @since 2021/11/23
 */
@Component
final class PrepareCompareItemBuilder {

    @Autowired
    SqlParseHandler sqlParseHandler;

    CompareItem build(AREXMocker instance) {
        MockCategoryType categoryType = instance.getCategoryType();
        String operationKey = operationName(categoryType, instance.getTargetRequest());
        if (StringUtils.isEmpty(operationKey)) {
            operationKey = instance.getOperationName();
        }
        ObjectNode obj;
        if (categoryType.isEntryPoint()) {
            obj = this.buildAttributes(instance.getTargetResponse());
        } else if (Objects.equals(categoryType.getName(), MockCategoryType.DATABASE.getName())) {
            // when the mock type is database, parse the field of "body"
            obj = this.buildAttributesAndParseBody(instance.getTargetRequest());
        } else {
            obj = this.buildAttributes(instance.getTargetRequest());
        }
        return new CompareItemImpl(operationKey, obj.toString());
    }

    private String operationName(MockCategoryType categoryType, Target target) {
        if (Objects.equals(categoryType, MockCategoryType.DATABASE)) {
            return target.attributeAsString(MockAttributeNames.DB_NAME);
        }
        if (Objects.equals(categoryType, MockCategoryType.REDIS)) {
            return target.attributeAsString(MockAttributeNames.CLUSTER_NAME);
        }
        return null;
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

    private ObjectNode buildAttributesAndParseBody(Target target) {
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
            obj.put("parsedBody", sqlParseHandler.sqlParse(target.getBody()));
        }
        return obj;
    }

    private final static class CompareItemImpl implements CompareItem {
        private final String compareMessage;
        private final String compareOperation;
        private final String compareService;

        private CompareItemImpl(String compareOperation, String compareMessage) {
            this(compareOperation, compareMessage, null);
        }

        private CompareItemImpl(String compareOperation, String compareMessage, String compareService) {
            this.compareMessage = compareMessage;
            this.compareOperation = compareOperation;
            this.compareService = compareService;
        }

        @Override
        public String getCompareContent() {
            return compareMessage;
        }

        @Override
        public String getCompareOperation() {
            return compareOperation;
        }

        @Override
        public String getCompareService() {
            return compareService;
        }
    }
}
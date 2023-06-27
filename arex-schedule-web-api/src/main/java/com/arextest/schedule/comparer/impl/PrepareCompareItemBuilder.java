package com.arextest.schedule.comparer.impl;

import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.schedule.comparer.CompareItem;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
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

    CompareItem build(AREXMocker instance) {
        MockCategoryType categoryType = instance.getCategoryType();
        String operationKey = operationName(categoryType, instance.getTargetRequest());
        if (StringUtils.isEmpty(operationKey)) {
            operationKey = instance.getOperationName();
        }
        long createTime = instance.getCreationTime();
        String body;
        String compareKey = instance.getId();
        boolean entryPointCategory = false;
        if (categoryType.isEntryPoint()) {
            body = Objects.isNull(instance.getTargetResponse()) ? null : instance.getTargetResponse().getBody();
            compareKey = null;
            entryPointCategory = true;
        } else if (Objects.equals(categoryType.getName(), MockCategoryType.DATABASE.getName())) {
            body = this.buildAttributes(instance.getTargetRequest()).toString();
        } else {
            body = Objects.isNull(instance.getTargetRequest()) ? null : instance.getTargetRequest().getBody();
        }
        return new CompareItemImpl(operationKey, body, compareKey, createTime, entryPointCategory);
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

    private final static class CompareItemImpl implements CompareItem {
        private final String compareMessage;
        private final String compareOperation;
        private final String compareService;
        private final String compareKey;
        private final long createTime;
        private final boolean entryPointCategory;

        private CompareItemImpl(String compareOperation, String compareMessage, String compareKey, long createTime, boolean entryPointCategory) {
            this(compareOperation, compareMessage, null, compareKey, createTime, entryPointCategory);
        }

        private CompareItemImpl(String compareOperation, String compareMessage, String compareService, String compareKey,
                                long createTime, boolean entryPointCategory) {
            this.compareMessage = compareMessage;
            this.compareOperation = compareOperation;
            this.compareService = compareService;
            this.compareKey = compareKey;
            this.createTime = createTime;
            this.entryPointCategory = entryPointCategory;
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

        @Override
        public String getCompareKey() {
            return compareKey;
        }

        @Override
        public long getCreateTime() {
            return createTime;
        }

        @Override
        public boolean isEntryPointCategory() {
            return entryPointCategory;
        }
    }
}
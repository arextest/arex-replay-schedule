package com.arextest.replay.schedule.comparer.impl;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.arextest.replay.schedule.common.CommonConstant;
import com.arextest.replay.schedule.comparer.CompareItem;
import com.arextest.storage.model.mocker.impl.ABtMocker;
import com.arextest.storage.model.mocker.impl.DalResultMocker;
import com.arextest.storage.model.mocker.impl.DatabaseMocker;
import com.arextest.storage.model.mocker.impl.DynamicResultMocker;
import com.arextest.storage.model.mocker.impl.HttpClientMocker;
import com.arextest.storage.model.mocker.impl.HttpMocker;
import com.arextest.storage.model.mocker.impl.MessageMocker;
import com.arextest.storage.model.mocker.impl.QmqConsumerMocker;
import com.arextest.storage.model.mocker.impl.QmqProducerMocker;
import com.arextest.storage.model.mocker.impl.RedisMocker;
import com.arextest.storage.model.mocker.impl.ServletMocker;
import com.arextest.storage.model.mocker.impl.SoaExternalMocker;
import com.arextest.storage.model.mocker.impl.SoaMainMocker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author jmo
 * @since 2021/11/23
 */
@Component
final class PrepareCompareItemBuilder {

    CompareItem build(Object instance) {
        if (instance instanceof DatabaseMocker) {
            return from((DatabaseMocker) instance);
        }
        if (instance instanceof ServletMocker) {
            return from((ServletMocker) instance);
        }
        if (instance instanceof HttpClientMocker) {
            return from((HttpClientMocker) instance);
        }
        if (instance instanceof ABtMocker) {
            return from((ABtMocker) instance);
        }
        if (instance instanceof DalResultMocker) {
            return from((DalResultMocker) instance);
        }
        if (instance instanceof DynamicResultMocker) {
            return from((DynamicResultMocker) instance);
        }
        if (instance instanceof QmqConsumerMocker) {
            return from((MessageMocker) instance);
        }
        if (instance instanceof QmqProducerMocker) {
            return from((MessageMocker) instance);
        }
        if (instance instanceof SoaExternalMocker) {
            return from((SoaExternalMocker) instance);
        }
        if (instance instanceof SoaMainMocker) {
            return from((SoaMainMocker) instance);
        }
        if (instance instanceof RedisMocker) {
            return from((RedisMocker) instance);
        }
        if (instance instanceof HttpMocker) {
            return from((HttpMocker) instance);
        }
        return null;
    }

    private CompareItem from(ABtMocker aBtMocker) {
        return new CompareItemImpl(aBtMocker.getExpCode(), aBtMocker.getVersion());
    }

    private CompareItem from(DalResultMocker instance) {
        ObjectNode obj = JsonNodeFactory.instance.objectNode();
        obj.put("database", instance.getDatabase());
        obj.put("sql", instance.getSql());
        obj.put("parameter", instance.getParameter());
        return new CompareItemImpl(instance.getDatabase(), obj.toString());
    }

    private CompareItem from(DatabaseMocker instance) {
        ObjectNode obj = JsonNodeFactory.instance.objectNode();
        obj.put("database", instance.getDbName());
        obj.put("sql", instance.getSql());
        obj.put("parameter", instance.getParameters());
        return new CompareItemImpl(instance.getDbName(), obj.toString());
    }

    private CompareItem from(DynamicResultMocker instance) {
        ObjectNode obj = JsonNodeFactory.instance.objectNode();
        obj.put("clazzName", instance.getClazzName());
        obj.put("operation", instance.getOperation());
        obj.put("operationKey", instance.getOperationKey());
        String compareOperation = instance.getClazzName() + CommonConstant.DOT + instance.getOperation();
        return new CompareItemImpl(compareOperation, obj.toString());
    }

    private CompareItem from(HttpClientMocker instance) {
        return new CompareItemImpl(instance.getUrl(), StringUtils.EMPTY, StringUtils.EMPTY);
    }

    private CompareItem from(ServletMocker instance) {
        return new CompareItemImpl(instance.getPath(), instance.getResponse(), instance.getPattern());
    }

    private CompareItem from(HttpMocker instance) {
        return new CompareItemImpl(instance.getOperation(), instance.getRequest(), instance.getService());
    }

    private CompareItem from(MessageMocker instance) {
        return new CompareItemImpl(instance.getSubject(), instance.getMsgBody());
    }

    private CompareItem from(SoaExternalMocker instance) {
        return new CompareItemImpl(instance.getOperation(), instance.getRequest());
    }

    private CompareItem from(SoaMainMocker instance) {
        return new CompareItemImpl(instance.getOperation(), instance.getResponse());
    }

    private CompareItem from(RedisMocker instance) {
        return new CompareItemImpl(instance.getClusterName(), instance.getRedisKey());
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

package com.arextest.schedule.model;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.schedule.model.dao.mongodb.ReplayRunDetailsCollection;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

/**
 * @author jmo
 * @see ReplayRunDetailsCollection
 * @since 2021/9/15
 */
@Data
@ToString(of = {"id", "recordId", "targetResultId", "sourceResultId", "sendStatus"})
@FieldNameConstants
@EqualsAndHashCode(of = {"id"})
public class ReplayActionCaseItem {
    private String id;
    private String planItemId;
    private String planId;
    private String recordId;
    private String targetResultId;
    private String sourceResultId;
    private String contextIdentifier;

    private ReplayActionItem parent;
    /**
     * @see CaseSendStatusType
     */
    @JsonIgnore
    private int sendStatus;
    /**
     * @see CompareProcessStatusType
     */
    private int compareStatus;
    private String caseType;
    @JsonIgnore
    private long recordTime;
    @JsonIgnore
    private Target targetRequest;
    @JsonIgnore
    private String messageId;
    @JsonIgnore
    private String sourceProvider;
    /**
     * the time the case actually starts executing
     */
    private long executionStartMillis;

    /**
     * the compare mode of the case, {@link CompareModeType}
     */
    @JsonIgnore
    private CompareModeType compareMode = CompareModeType.QUiCK;

    public String replayDependency() {
        return requestAttribute(MockAttributeNames.CONFIG_BATCH_NO);
    }

    public String requestMessage() {
        return this.targetRequest == null ? null : this.targetRequest.getBody();
    }

    public String consumeGroup() {
        return requestAttribute(MockAttributeNames.CONSUMER_GROUP_NAME);
    }

    public String requestMessageFormat() {
        return requestAttribute(MockAttributeNames.CONTENT_TYPE);
    }

    public Map<String, String> requestHeaders() {
        if (this.targetRequest != null) {
            Object v = targetRequest.getAttribute("Headers");
            if (v instanceof String) {
                return JSONObject.parseObject((String)v, new TypeReference<Map<String, String>>() {});
            }
            String json = JSONObject.toJSONString(v);
            return JSONObject.parseObject(json, new TypeReference<Map<String, String>>() {});
        }
        return null;
    }

    public String requestMethod() {
        return requestAttribute(MockAttributeNames.HTTP_METHOD);
    }

    public String requestPath() {
        return requestAttribute(MockAttributeNames.REQUEST_PATH);
    }

    private String requestAttribute(String name) {
        if (this.targetRequest != null) {
            return targetRequest.attributeAsString(name);
        }
        return null;
    }

    public String getPlanItemId() {
        if (this.parent != null) {
            return this.parent.getId();
        }
        return planItemId;
    }

    @JsonIgnore
    private String sendErrorMessage;

    public void buildParentErrorMessage(String otherErrorMessage) {
        this.parent
            .setErrorMessage(StringUtils.isNotEmpty(this.sendErrorMessage) ? this.sendErrorMessage : otherErrorMessage);
        this.parent.getParent()
            .setErrorMessage(StringUtils.isNotEmpty(this.sendErrorMessage) ? this.sendErrorMessage : otherErrorMessage);
    }
}
package com.arextest.replay.schedule.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;

import java.util.Map;

/**
 * @author jmo
 * @since 2021/9/15
 */
@Data
@ToString(of = {"id", "recordId", "targetResultId", "sourceResultId", "sendStatus"})
public class ReplayActionCaseItem {
    private long id;
    private long planItemId;
    private String recordId;
    private String targetResultId;
    private String sourceResultId;

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
    private int caseType;
    @JsonIgnore
    private long recordTime;
    private String replayDependency;
    @JsonIgnore
    private String requestMessage;
    @JsonIgnore
    private String consumeGroup;
    @JsonIgnore
    private String requestMessageFormat;
    @JsonIgnore
    private Map<String, String> requestHeaders;
    @JsonIgnore
    private String requestMethod;
    @JsonIgnore
    private String requestPath;

    public long getPlanItemId() {
        if (this.parent != null) {
            return this.parent.getId();
        }
        return planItemId;
    }

    @JsonIgnore
    private String sendErrorMessage;
}

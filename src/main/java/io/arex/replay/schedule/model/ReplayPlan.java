package io.arex.replay.schedule.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;

import java.util.Date;
import java.util.List;

/**
 * @author jmo
 * @since 2021/9/15
 */
@Data
@ToString(of = {"id", "appId", "sourceEnv", "sourceHost", "targetEnv", "targetHost"})
public class ReplayPlan {
    private long id;
    private String appId;
    private int replaySendMaxQps;
    @JsonIgnore
    private String planName;
    @JsonIgnore
    private String sourceEnv;
    @JsonIgnore
    private String targetEnv;
    @JsonIgnore
    private String sourceHost;
    @JsonIgnore
    private String targetHost;
    @JsonIgnore
    private String targetImageId;
    @JsonIgnore
    private String targetImageName;
    @JsonIgnore
    private Date caseSourceFrom;
    @JsonIgnore
    private Date caseSourceTo;
    @JsonIgnore
    private Date planCreateTime;
    @JsonIgnore
    private Date planFinishTime;
    @JsonIgnore
    private String operator;
    @JsonIgnore
    private String arexCordVersion;
    @JsonIgnore
    private String arexExtVersion;
    @JsonIgnore
    private String caseRecordVersion;
    private int caseTotalCount;
    /**
     * see {@link CaseSourceEnvType}
     */
    @JsonIgnore
    private int caseSourceType;
    @JsonIgnore
    private List<ReplayActionItem> replayActionItemList;
    @JsonIgnore
    private String appName;

}

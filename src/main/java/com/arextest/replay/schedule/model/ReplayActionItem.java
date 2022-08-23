package com.arextest.replay.schedule.model;

import com.arextest.replay.schedule.common.SendSemaphoreLimiter;
import com.arextest.replay.schedule.model.dao.mongodb.ReplayPlanItemCollection;
import com.arextest.replay.schedule.model.deploy.ServiceInstance;
import com.arextest.replay.schedule.model.deploy.ServiceInstanceOperation;
import com.arextest.storage.model.enums.MockCategoryType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author jmo
 * @see ReplayPlanItemCollection
 * @since 2021/9/15
 */
@Data
public class ReplayActionItem {
    public final static int SOA_TRIGGER = 0;
    public final static int QMQ_TRIGGER = 1;
    private ReplayPlan parent;
    private String id;
    private String operationId;
    private String planId;
    /**
     * example: applyRefund
     */
    @JsonIgnore
    private String operationName;
    /**
     * example: RefundPaymentService
     */
    @JsonIgnore
    private String serviceName;
    /**
     * example: flight.ticket.refund.refundpaymentservice.v1.refundpaymentservice
     */
    private String serviceKey;
    @JsonIgnore
    private List<ReplayActionCaseItem> caseItemList;
    @JsonIgnore
    private long lastRecordTime;

    @JsonIgnore
    private SendSemaphoreLimiter sendRateLimiter;
    /**
     * 初始待执行=0，执行中=1，执行结束=2
     *
     * @see ReplayStatusType
     */
    @JsonIgnore
    private int replayStatus;
    @JsonIgnore
    private Date replayBeginTime;
    @JsonIgnore
    private Date replayFinishTime;

    /**
     * see defined {@link MockCategoryType}'s all main entry codeValue
     * SOA=0，QMQ=1
     */
    private int actionType;
    @JsonIgnore
    private ServiceInstance sourceInstance;
    @JsonIgnore
    private ServiceInstance targetInstance;
    @JsonIgnore
    private ServiceInstanceOperation mappedInstanceOperation;
    private int replayCaseCount;
    private String appId;

    public String getPlanId() {
        if (this.parent != null) {
            return this.parent.getId();
        }
        return planId;
    }

    public String getAppId() {
        if (appId == null && this.parent != null) {
            return this.parent.getAppId();
        }
        return appId;
    }

    public boolean isEmpty() {
        return replayCaseCount == 0;
    }

    public boolean finished() {
        return replayStatus == ReplayStatusType.FINISHED.getValue();
    }
}


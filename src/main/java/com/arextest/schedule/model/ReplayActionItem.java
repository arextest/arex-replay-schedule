package com.arextest.schedule.model;

import com.arextest.schedule.common.SendSemaphoreLimiter;
import com.arextest.schedule.model.dao.mongodb.ReplayPlanItemCollection;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.model.deploy.ServiceInstanceOperation;
import com.arextest.model.mock.MockCategoryType;
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
     * @see ReplayStatusType
     */
    @JsonIgnore
    private int replayStatus;
    private Date replayBeginTime;
    @JsonIgnore
    private Date replayFinishTime;

    /**
     * see defined {@link MockCategoryType} for all entry points
     */
    private String actionType;
    @JsonIgnore
    private List<ServiceInstance> sourceInstance;
    @JsonIgnore
    private List<ServiceInstance> targetInstance;
    @JsonIgnore
    private ServiceInstanceOperation mappedInstanceOperation;
    private int replayCaseCount;
    private String appId;
    /**
     * the interfaces which don't use the mock when replaying
     */
    @JsonIgnore
    private String exclusionOperationConfig;

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

    @JsonIgnore
    private String errorMessage;
}
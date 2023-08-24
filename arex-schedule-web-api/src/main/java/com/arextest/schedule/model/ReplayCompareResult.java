package com.arextest.schedule.model;


import com.arextest.diff.model.CompareResult;
import com.arextest.diff.model.enumeration.DiffResultCode;
import com.arextest.diff.model.log.LogEntity;
import com.arextest.diff.model.MsgInfo;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Created by wang_yc on 2021/6/3
 */
@Data
@FieldNameConstants
public class ReplayCompareResult {
    private String id;
    private String planId;
    private String planItemId;
    private String caseId;

    private String operationId;
    private String categoryName;
    private String operationName;
    private String serviceName;
    private String recordId;
    private String replayId;

    /**
     * see {@link DiffResultCode} &amp; {@link CaseSendStatusType}
     */
    private int diffResultCode;
    private String baseMsg;
    private String testMsg;
    private long recordTime;
    private long replayTime;
    private String instanceId;
    private List<LogEntity> logs;
    private MsgInfo msgInfo;

    public static ReplayCompareResult createFrom(ReplayActionCaseItem caseItem) {
        ReplayCompareResult newResult = new ReplayCompareResult();
        newResult.setRecordId(caseItem.getRecordId());
        newResult.setCaseId(caseItem.getId());
        newResult.setReplayId(caseItem.getTargetResultId());
        newResult.setPlanItemId(caseItem.getPlanItemId());
        newResult.setPlanId(caseItem.getParent().getParent().getId());
        newResult.setOperationId(caseItem.getParent().getOperationId());
        if (StringUtils.isNotBlank(caseItem.getParent().getServiceKey())) {
            newResult.setServiceName(caseItem.getParent().getServiceKey());
        }
        return newResult;
    }

    public static ReplayCompareResult createFrom(ReplayActionCaseItem caseItem, CompareResult sdkResult) {
        ReplayCompareResult newResult = createFrom(caseItem);
        newResult.setOperationName(caseItem.getParent().getOperationName());
        newResult.setMsgInfo(sdkResult.getMsgInfo());
        newResult.setCategoryName(caseItem.getCaseType());
        newResult.setBaseMsg(sdkResult.getProcessedBaseMsg());
        newResult.setTestMsg(sdkResult.getProcessedTestMsg());
        newResult.setLogs(sdkResult.getLogs());
        newResult.setDiffResultCode(sdkResult.getCode());
        return newResult;
    }
}
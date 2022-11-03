package com.arextest.schedule.model;


import com.arextest.diff.model.CompareResult;
import com.arextest.diff.model.enumeration.DiffResultCode;
import com.arextest.diff.model.log.LogEntity;
import com.arextest.storage.model.enums.MockCategoryType;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Created by wang_yc on 2021/6/3
 */
@Data
public class ReplayCompareResult {
    private String planId;
    private String planItemId;
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
    private List<LogEntity> logs;

    public static ReplayCompareResult createFrom(ReplayActionCaseItem caseItem) {
        ReplayCompareResult newResult = new ReplayCompareResult();
        newResult.setRecordId(caseItem.getRecordId());
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
        MockCategoryType mockCategoryType = MockCategoryType.of(caseItem.getCaseType());
        if (mockCategoryType == null) {
            newResult.setCategoryName(String.valueOf(caseItem.getCaseType()));
        } else {
            newResult.setCategoryName(mockCategoryType.getDisplayName());
        }
        newResult.setBaseMsg(sdkResult.getProcessedBaseMsg());
        newResult.setTestMsg(sdkResult.getProcessedTestMsg());
        newResult.setLogs(sdkResult.getLogs());
        newResult.setDiffResultCode(sdkResult.getCode());
        return newResult;
    }
}
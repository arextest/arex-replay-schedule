package com.arextest.schedule.comparer;

import com.arextest.schedule.model.ReplayCompareResult;
import com.arextest.schedule.model.ReplayActionCaseItem;

import java.util.List;

/**
 * @author jmo
 * @since 2022/1/27
 */
public interface ComparisonWriter {
    boolean writeIncomparable(ReplayActionCaseItem caseItem, String remark);

    boolean write(List<ReplayCompareResult> comparedResult);
}
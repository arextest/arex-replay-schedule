package com.arextest.replay.schedule.comparer;

import com.arextest.replay.schedule.model.ReplayCompareResult;
import com.arextest.replay.schedule.model.ReplayActionCaseItem;

import java.util.List;

/**
 * @author jmo
 * @since 2022/1/27
 */
public interface ComparisonWriter {
    boolean writeIncomparable(ReplayActionCaseItem caseItem, String remark);

    boolean write(List<ReplayCompareResult> comparedResult);
}

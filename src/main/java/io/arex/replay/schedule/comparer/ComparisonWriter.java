package io.arex.replay.schedule.comparer;

import io.arex.replay.schedule.model.ReplayCompareResult;
import io.arex.replay.schedule.model.ReplayActionCaseItem;

import java.util.List;

/**
 * @author jmo
 * @since 2022/1/27
 */
public interface ComparisonWriter {
    boolean writeIncomparable(ReplayActionCaseItem caseItem, String remark);

    boolean write(List<ReplayCompareResult> comparedResult);
}

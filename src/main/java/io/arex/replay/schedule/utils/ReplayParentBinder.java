package io.arex.replay.schedule.utils;

import io.arex.replay.schedule.model.ReplayActionCaseItem;
import io.arex.replay.schedule.model.ReplayActionItem;
import io.arex.replay.schedule.model.ReplayPlan;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author jmo
 * @since 2021/10/12
 */
public final class ReplayParentBinder {
    private ReplayParentBinder() {

    }

    public static void setupReplayActionParent(List<ReplayActionItem> replayActionItemList, ReplayPlan replayPlan) {
        if (CollectionUtils.isEmpty(replayActionItemList)) {
            return;
        }
        for (ReplayActionItem actionItem : replayActionItemList) {
            actionItem.setParent(replayPlan);
        }
    }

    public static void setupCaseItemParent(List<ReplayActionCaseItem> sourceItemList, ReplayActionItem parent) {
        if (CollectionUtils.isEmpty(sourceItemList)) {
            return;
        }
        for (ReplayActionCaseItem caseItem : sourceItemList) {
            if (StringUtils.isEmpty(caseItem.getRequestMethod())) {
                caseItem.setRequestMethod("POST");
            }
            caseItem.setCaseType(parent.getActionType());
            caseItem.setParent(parent);
        }
    }
}

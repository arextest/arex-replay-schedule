package io.arex.replay.schedule.utils;

import io.arex.replay.schedule.model.ReplayActionCaseItem;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * Created by wang_yc on 2021/10/21
 */
public final class ReplayRequestFormatBinder {

    private ReplayRequestFormatBinder() {

    }

    public static void setupCaseRequestFormat(List<ReplayActionCaseItem> sourceItemList, String requestFormat) {
        if (CollectionUtils.isEmpty(sourceItemList)) {
            return;
        }
        for (ReplayActionCaseItem caseItem : sourceItemList) {
            if (caseItem.getRequestMessage().startsWith("{")) {
                caseItem.setRequestMessageFormat(requestFormat);
            }
        }
    }
}

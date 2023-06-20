package com.arextest.schedule.spi;

import com.arextest.schedule.spi.model.ReplayActionCaseItem;
import com.arextest.schedule.spi.model.ReplayInvokeResult;

public interface ReplaySenderExtension {
    String getName();

    /**
     * remote invoke.
     * @param request ReplayActionCaseItem's extension
     * @return replayResult with traceId.
     */
    ReplayInvokeResult invoke(ReplayActionCaseItem request);
}

package com.arextest.schedule.spi;

import com.arextest.schedule.spi.model.ReplayInvokeRequest;
import com.arextest.schedule.spi.model.ReplayInvokeResult;

public interface ReplayInvokerExtension {
    String getName();

    boolean isSupported(String caseType);

    /**
     * remote invoke.
     * @param request ReplayActionCaseItem's extension
     * @return replayResult with traceId.
     */
    ReplayInvokeResult invoke(ReplayInvokeRequest request);
}

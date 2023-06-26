package com.arextest.schedule.extension.invoker;

import com.arextest.schedule.extension.model.ReplayInvokeResult;

public interface ReplayExtensionInvoker {
    boolean isSupported(String caseType);

    ReplayInvokeResult invoke(ReplayInvocation invocation);

    default int order() {
        return 1;
    }
}

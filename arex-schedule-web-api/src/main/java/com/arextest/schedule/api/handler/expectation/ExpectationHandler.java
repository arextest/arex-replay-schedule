package com.arextest.schedule.api.handler.expectation;

import com.arextest.schedule.model.ReplayActionCaseItem;

public interface ExpectationHandler {
    void handle(ReplayActionCaseItem caseItem);
}

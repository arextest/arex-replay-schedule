package com.arextest.schedule.result.expectation;

import com.arextest.schedule.api.handler.expectation.ExpectationHandler;
import com.arextest.schedule.api.listener.ReplayResultListener;
import com.arextest.schedule.model.ReplayActionCaseItem;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
@DependsOn("prepareDataListener")
public class ExpectationListenerImpl implements ReplayResultListener {
    @Resource
    private List<ExpectationHandler> expectationHandlerList;

    @Override
    public void notify(ReplayActionCaseItem caseItem) {
        // validate data
        if (!validate(caseItem)) {
            return;
        }

        for (ExpectationHandler handler : expectationHandlerList) {
            handler.handle(caseItem);
        }
    }

    private static boolean validate(ReplayActionCaseItem caseItem) {
        if (caseItem.getReplayCaseContext() == null ||
            CollectionUtils.isEmpty(caseItem.getReplayCaseContext().getReplayCaseResultList())) {
            return false;
        }
        if (caseItem.getParent() == null || CollectionUtils.isEmpty(caseItem.getParent().getExpectationScriptList())) {
            return false;
        }
        return true;
    }
}

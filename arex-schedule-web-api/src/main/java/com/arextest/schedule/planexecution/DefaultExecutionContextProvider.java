package com.arextest.schedule.planexecution;

import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayPlan;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.*;

/**
 * Created by Qzmo on 2023/5/15
 *
 * Default implementation illustrating functionalities of execution context
 */
@Slf4j
public class DefaultExecutionContextProvider implements PlanExecutionContextProvider<Map<String, String>> {

    private final static String REMOTE_CONFIG_DEP_KEY = "CONFIG_VER";
    private final static String DEFAULT_CONTEXT_NAME = "DEFAULT_EXECUTION_CONTEXT";

    @Override
    public List<PlanExecutionContext<Map<String, String>>> buildContext(ReplayPlan plan) {
        PlanExecutionContext<Map<String, String>> singletonContext = new PlanExecutionContext<>();
        singletonContext.setContextName(DEFAULT_CONTEXT_NAME);

        // store dependency version for further usage
        HashMap<String, String> dependencies = new HashMap<>();
        dependencies.put(REMOTE_CONFIG_DEP_KEY, "EXAMPLE_VER");
        singletonContext.setDependencies(dependencies);

        // set up base query for cases belonging to this context
        Criteria dummyCriteria = Criteria.where("dataChangeCreateTime").gt(0);
        singletonContext.setContextCaseQuery(Collections.singletonList(dummyCriteria));

        List<PlanExecutionContext<Map<String, String>>> contexts = Collections.singletonList(singletonContext);
        LOGGER.info("Constructed contexts of size: {}", contexts.size());

        return contexts;
    }

    @Override
    public void injectContextIntoCase(List<ReplayActionCaseItem> cases) {
        cases.forEach(caseItem -> {
            caseItem.setContextIdentifier(StringUtils.EMPTY);
        });
    }

    @Override
    public void onBeforeContextExecution(PlanExecutionContext<Map<String, String>> currentContext, ReplayPlan plan) {
        MDCTracer.addExecutionContextNme(currentContext.getContextName());
        LOGGER.info("Start executing context: {}", currentContext);
        // prepare dependencies before sending any cases of this context...
    }

    @Override
    public void onAfterContextExecution(PlanExecutionContext<Map<String, String>> currentContext, ReplayPlan plan) {
        LOGGER.info("Finished executing context: {}", currentContext);
        // clean up context related resources on target instances...
    }
}

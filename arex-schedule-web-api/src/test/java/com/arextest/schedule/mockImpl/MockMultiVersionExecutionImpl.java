package com.arextest.schedule.mockImpl;

import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.ExecutionContextActionType;
import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.planexecution.PlanExecutionContextProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.*;


/**
 * Created by Qzmo on 2023/5/15
 * For test usage only, to simulate multiple version of execution context
 */
@Slf4j
public class MockMultiVersionExecutionImpl implements PlanExecutionContextProvider<Map<String, String>> {

    private final static String REMOTE_CONFIG_DEP_KEY = "CONFIG_VER";
    private final static String DEFAULT_CONTEXT_NAME = "DEFAULT_EXECUTION_CONTEXT";

    @Override
    public List<PlanExecutionContext<Map<String, String>>> buildContext(ReplayPlan plan) {

        List<PlanExecutionContext<Map<String, String>>> contexts = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            PlanExecutionContext<Map<String, String>> singletonContext = new PlanExecutionContext<>();
            singletonContext.setContextName(DEFAULT_CONTEXT_NAME + i);

            // store dependency version for further usage
            HashMap<String, String> dependencies = new HashMap<>();
            dependencies.put(REMOTE_CONFIG_DEP_KEY, "EXAMPLE_VER" + i);
            singletonContext.setDependencies(dependencies);

            // set up base query for cases belonging to this context
            Criteria dummyCriteria = Criteria.where(ReplayActionCaseItem.Fields.contextIdentifier).is(String.valueOf(i));
            singletonContext.setContextCaseQuery(Collections.singletonList(dummyCriteria));
            contexts.add(singletonContext);
        }

        return contexts;
    }

    @Override
    public void injectContextIntoCase(List<ReplayActionCaseItem> cases) {
        cases.forEach(caseItem -> {
            caseItem.setContextIdentifier(new Random().nextInt(10) + StringUtils.EMPTY);
        });
    }

    @Override
    public void onBeforeContextExecution(PlanExecutionContext<Map<String, String>> currentContext, ReplayPlan plan) {
        MDCTracer.addExecutionContextNme(currentContext.getContextName());
        currentContext.setActionType(new Random().nextInt(10) >= 5 ? ExecutionContextActionType.SKIP_CASE_OF_CONTEXT : ExecutionContextActionType.NORMAL);
        LOGGER.info("Start executing context: {}", currentContext);
        // prepare dependencies before sending any cases of this context...
    }

    @Override
    public void onAfterContextExecution(PlanExecutionContext<Map<String, String>> currentContext, ReplayPlan plan) {
        LOGGER.info("Finished executing context: {}", currentContext);
        // clean up context related resources on target instances...
    }
}

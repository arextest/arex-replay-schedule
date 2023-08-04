package com.arextest.schedule.planexecution.impl;

import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.planexecution.PlanExecutionContextProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Qzmo on 2023/5/15
 *
 * Default implementation illustrating functionalities of execution context
 */
@Slf4j
@AllArgsConstructor
public class DefaultExecutionContextProvider implements PlanExecutionContextProvider<DefaultExecutionContextProvider.ContextDependenciesHolder> {
    private final ReplayActionCaseItemRepository replayActionCaseItemRepository;

    private static final String CONTEXT_PREFIX = "batch-";
    @Data
    static class ContextDependenciesHolder {
        private String contextIdentifier;
    }

    @Override
    public List<PlanExecutionContext<ContextDependenciesHolder>> buildContext(ReplayPlan plan) {
        Set<String> distinctIdentifiers = replayActionCaseItemRepository.getAllContextIdentifiers(plan.getId());
        List<PlanExecutionContext<ContextDependenciesHolder>> contexts = new ArrayList<>();

        if (replayActionCaseItemRepository.hasNullIdentifier(plan.getId())) {
            // build context for null identifier, will skip before hook for this context
            PlanExecutionContext<ContextDependenciesHolder> context = new PlanExecutionContext<>();
            ContextDependenciesHolder dependenciesHolder = new ContextDependenciesHolder();
            dependenciesHolder.setContextIdentifier(null);
            context.setContextName(CONTEXT_PREFIX + "no-config");
            context.setDependencies(dependenciesHolder);
            contexts.add(context);
        }

        // build context for each distinct identifier, need to prepare remote resources for each context
        distinctIdentifiers.forEach(identifier -> {
            PlanExecutionContext<ContextDependenciesHolder> context = new PlanExecutionContext<>();
            ContextDependenciesHolder dependenciesHolder = new ContextDependenciesHolder();
            dependenciesHolder.setContextIdentifier(identifier);
            context.setContextName(CONTEXT_PREFIX + identifier);
            context.setDependencies(dependenciesHolder);
            contexts.add(context);
        });

        return contexts;
    }

    @Override
    public void injectContextIntoCase(List<ReplayActionCaseItem> cases) {
        cases.forEach(caseItem -> {
            // extract config batch no from caseItem in advance, the entire request will be compressed using zstd which is not queryable
            caseItem.setContextIdentifier(caseItem.replayDependency());
        });
    }

    @Override
    public void onBeforeContextExecution(PlanExecutionContext<ContextDependenciesHolder> currentContext, ReplayPlan plan) {
        MDCTracer.addExecutionContextNme(currentContext.getContextName());
        LOGGER.info("Start executing context: {}", currentContext);
        // prepare dependencies before sending any cases of this context...
    }

    @Override
    public void onAfterContextExecution(PlanExecutionContext<ContextDependenciesHolder> currentContext, ReplayPlan plan) {
        LOGGER.info("Finished executing context: {}", currentContext);
        // clean up context related resources on target instances...
    }
}

package com.arextest.schedule.planexecution.impl;

import com.arextest.schedule.bizlog.BizLogger;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.ExecutionContextActionType;
import com.arextest.schedule.model.PlanExecutionContext;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.planexecution.PlanExecutionContextProvider;
import com.arextest.schedule.sender.ReplaySender;
import com.arextest.schedule.sender.ReplaySenderFactory;
import com.arextest.schedule.utils.ReplayParentBinder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.retry.support.RetryTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Qzmo on 2023/5/15
 * <p>
 * Default implementation illustrating functionalities of execution context
 */
@Slf4j
@AllArgsConstructor
public class DefaultExecutionContextProvider implements PlanExecutionContextProvider<DefaultExecutionContextProvider.ContextDependenciesHolder> {
    private final ReplayActionCaseItemRepository replayActionCaseItemRepository;
    private final ReplaySenderFactory replaySenderFactory;

    private static final RetryTemplate RETRY_TEMPLATE = RetryTemplate.builder().maxAttempts(3).build();
    private static final String CONTEXT_PREFIX = "batch-";
    private static final String CONFIG_CENTER_WARM_UP_HEAD = "arex_replay_prepare_dependency";

    @Data
    public static class ContextDependenciesHolder {
        private String contextIdentifier;
    }

    @Override
    public List<PlanExecutionContext<ContextDependenciesHolder>> buildContext(ReplayPlan plan) {
        Set<String> distinctIdentifiers = replayActionCaseItemRepository.getAllContextIdentifiers(plan.getId());
        List<PlanExecutionContext<ContextDependenciesHolder>> contexts = new ArrayList<>();
        boolean hasNullIdentifier = replayActionCaseItemRepository.hasNullIdentifier(plan.getId());
        if (hasNullIdentifier) {
            // build context for null identifier, will skip before hook for this context
            PlanExecutionContext<ContextDependenciesHolder> context = new PlanExecutionContext<>();
            context.setContextName(CONTEXT_PREFIX + "no-config");

            // set up null dependency to indicate that this context does not need to be warmed up
            ContextDependenciesHolder dependenciesHolder = new ContextDependenciesHolder();
            dependenciesHolder.setContextIdentifier(null);
            context.setDependencies(dependenciesHolder);

            context.setContextCaseQuery(Collections.singletonList(
                    Criteria.where(ReplayActionCaseItem.Fields.contextIdentifier).isNull()));
            contexts.add(context);
        }

        // build context for each distinct identifier, need to prepare remote resources for each context
        distinctIdentifiers.forEach(identifier -> {
            PlanExecutionContext<ContextDependenciesHolder> context = new PlanExecutionContext<>();
            context.setContextName(CONTEXT_PREFIX + identifier);

            // set up dependency info holder for warmup
            ContextDependenciesHolder dependenciesHolder = new ContextDependenciesHolder();
            dependenciesHolder.setContextIdentifier(identifier);
            context.setDependencies(dependenciesHolder);

            // set up query for cases of this context
            context.setContextCaseQuery(Collections.singletonList(
                    Criteria.where(ReplayActionCaseItem.Fields.contextIdentifier).is(identifier)));
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
        ContextDependenciesHolder dependencyHolder = currentContext.getDependencies();

        if (StringUtils.isEmpty(dependencyHolder.getContextIdentifier())) {
            // skip before hook for null identifier
            return;
        }

        final Map<String, String> warmupHeader = new HashMap<>();
        warmupHeader.put(CONFIG_CENTER_WARM_UP_HEAD, dependencyHolder.getContextIdentifier());

        try {
            // find warmup case for this batch
            ReplayActionCaseItem warmupCase = replayActionCaseItemRepository.getOneOfContext(plan.getId(), dependencyHolder.getContextIdentifier());
            ReplayParentBinder.setupCaseItemParent(warmupCase, plan.getActionItemMap().get(warmupCase.getPlanItemId()));
            ReplaySender sender = replaySenderFactory.findReplaySender(warmupCase.getCaseType());

            // send warmup case to target instance
            RETRY_TEMPLATE.execute(context -> {
                // todo: multi-instance should be supported here
                boolean caseSuccess = sender.send(warmupCase, warmupHeader);
                if (!caseSuccess) {
                    throw new RuntimeException("Failed to warmup context: " + currentContext + " with case:" + warmupCase);
                }
                return true;
            });
        } catch (Throwable t) {
            // any error goes here are considered as fatal, needs to look into details
            currentContext.setActionType(ExecutionContextActionType.SKIP_CASE_OF_CONTEXT);
            BizLogger.recordContextPrepareFailure(currentContext, t);
            LOGGER.error("Failed to execute before hook for context: {}", currentContext, t);
        }
    }

    @Override
    public void onAfterContextExecution(PlanExecutionContext<ContextDependenciesHolder> currentContext, ReplayPlan plan) {
        LOGGER.info("Finished executing context: {}", currentContext);
        // clean up context related resources on target instances...
    }
}

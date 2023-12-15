package com.arextest.schedule.result.expectation;

import com.arextest.schedule.api.handler.expectation.ExpectationHandler;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.replay.ReplayCaseItem;
import com.arextest.schedule.model.replay.ReplayCaseResult;
import com.arextest.web.model.contract.contracts.config.expectation.ExpectationScriptModel;
import com.arextest.web.model.contract.contracts.config.expectation.ScriptExtractOperationModel;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class ExpectationHandlerImpl implements ExpectationHandler {

    private final ExpectationScriptEngine scriptEngine;

    public ExpectationHandlerImpl(ExpectationScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    @Override
    public void handle(ReplayActionCaseItem caseItem) {
        for (ExpectationScriptModel script : caseItem.getParent().getExpectationScriptList()) {
            String builtScript = buildScript(caseItem, script);
            scriptEngine.eval(builtScript);
        }
    }

    private String buildScript(ReplayActionCaseItem caseItem, ExpectationScriptModel script) {
        StringBuilder builder = new StringBuilder();
        builder.append("var arex = {");
        builder.append("assert:new ArexAssertion(\"").append(caseItem.getId()).append("\"),");
        int categoryIndex = 0;

        Map<String, List<ScriptExtractOperationModel>> map = script.getExtractOperationList().stream()
            .collect(Collectors.groupingBy(ScriptExtractOperationModel::getCategoryName));

        for (Map.Entry<String, List<ScriptExtractOperationModel>> entry : map.entrySet()) {
            if (categoryIndex > 0) {
                builder.append(",");
            }
            builder.append(entry.getKey()).append(": {");
            int operationIndex = 0;
            for (ScriptExtractOperationModel imports : entry.getValue()) {
                ReplayCaseItem replayCaseItem = getReplayCaseItem(imports.getCategoryName(), imports.getOperationName(),
                    caseItem.getReplayCaseContext().getReplayCaseResultList());
                if (replayCaseItem == null) {
                    continue;
                }
                if (operationIndex > 0) {
                    builder.append(",");
                }
                builder.append("\"").append(imports.getOperationName()).append("\": {");
                builder.append("request: ").append(jsonNormalize(replayCaseItem.getRequest())).append(",");
                builder.append("response: ").append(jsonNormalize(replayCaseItem.getResponse()));
                if (operationIndex < entry.getValue().size()) {
                    builder.append("}");
                }
                operationIndex++;
            }
            if (categoryIndex < script.getExtractOperationList().size()) {
                builder.append("}");
            }
            categoryIndex++;
        }
        builder.append("};");
        builder.append(script.getNormalizedContent());
        return builder.toString();
    }

    private String jsonNormalize(String request) {
        if (StringUtils.isEmpty(request)) {
            return "{}";
        }
        if (StringUtils.startsWith(request, "{") && StringUtils.endsWith(request, "}")) {
            return request;
        }
        return "{" + request + "}";
    }

    private ReplayCaseItem getReplayCaseItem(String category, String operationName,
        List<ReplayCaseResult> replayCaseResultList) {
        if (CollectionUtils.isEmpty(replayCaseResultList) || StringUtils.isEmpty(operationName)) {
            return null;
        }
        for (ReplayCaseResult replayCaseResult : replayCaseResultList) {
            if (!StringUtils.equals(category, replayCaseResult.getCategoryName())) {
                continue;
            }
            for (ReplayCaseItem replayCaseItem : replayCaseResult.getReplayCaseList()) {
                if (StringUtils.equals(operationName, replayCaseItem.getOperationName())) {
                    return replayCaseItem;
                }
            }
        }
        return null;
    }
}

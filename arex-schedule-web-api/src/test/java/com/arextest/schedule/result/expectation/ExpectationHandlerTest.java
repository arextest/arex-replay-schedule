package com.arextest.schedule.result.expectation;

import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.replay.ReplayCaseContext;
import com.arextest.schedule.model.replay.ReplayCaseItem;
import com.arextest.schedule.model.replay.ReplayCaseResult;
import com.arextest.web.model.contract.contracts.config.expectation.ExpectationScriptModel;
import com.arextest.web.model.contract.contracts.config.expectation.ScriptExtractOperationModel;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @since 2023/11/28
 */
class ExpectationHandlerTest {
    static ExpectationHandlerImpl handler;
    static ExpectationScriptEngine scriptEngine;
    static ExpectationService expectationService;
    @BeforeAll
    static void setUp() {
        expectationService = new ExpectationService();
        scriptEngine = new ExpectationScriptEngine(expectationService);
        handler = new ExpectationHandlerImpl(scriptEngine);
    }

    @AfterAll
    static void tearDown() {
        handler = null;
    }

    @Test
    void handle() {
        String normalizedContent = "var serviceConsumerA = arex.SoaConsumer[\"HelloService.ConsumerA\"];\narex.assert.equals(\"SoaConsumer\", \"HelloService.ConsumerA\", \"name\", 'arex.assert.equals(\"serviceConsumerB\", serviceConsumerA.request.name);', \"serviceConsumerB\", serviceConsumerA.request.name);\nvar serviceConsumerB = arex.SoaConsumer[\"HelloService.ConsumerB\"];\narex.assert.equals(\"SoaConsumer\", \"HelloService.ConsumerB\", \"name\", 'arex.assert.equals(\"serviceConsumerB\", serviceConsumerB.request.name);', \"serviceConsumerB\", serviceConsumerB.request.name);\narex.assert.equals(\"SoaProvider\", \"HelloService\", \"order.id\", 'arex.assert.equals(\"mark5\", arex.SoaProvider[\"HelloService\"].response.order.id);', \"mark5\", arex.SoaProvider[\"HelloService\"].response.order.id);";

        ExpectationScriptModel expectationScript = new ExpectationScriptModel();
        expectationScript.setAppId("test-service");
        expectationScript.setOperationId("test-operation-id");

        ScriptExtractOperationModel operationModel1 = new ScriptExtractOperationModel();
        operationModel1.setCategoryName("SoaConsumer");
        operationModel1.setVariableName("serviceConsumerA");
        operationModel1.setOperationName("HelloService.ConsumerA");
        operationModel1.setOriginalText("var serviceConsumerA = arex.SoaConsumer[\"HelloService.ConsumerA\"];");

        ScriptExtractOperationModel operationModel2 = new ScriptExtractOperationModel();
        operationModel2.setCategoryName("SoaConsumer");
        operationModel2.setVariableName("serviceConsumerB");
        operationModel2.setOperationName("HelloService.ConsumerB");
        operationModel2.setOriginalText("var serviceConsumerB = arex.SoaConsumer[\"HelloService.ConsumerB\"];");

        ScriptExtractOperationModel operationModel3 = new ScriptExtractOperationModel();
        operationModel3.setCategoryName("SoaProvider");
        operationModel3.setVariableName(null);
        operationModel3.setOperationName("HelloService");
        operationModel3.setOriginalText("arex.SoaProvider[\"HelloService\"]");

        List<ScriptExtractOperationModel> extractOperationList = new ArrayList<>();
        extractOperationList.add(operationModel1);
        extractOperationList.add(operationModel2);
        extractOperationList.add(operationModel3);
        expectationScript.setExtractOperationList(extractOperationList);
        expectationScript.setNormalizedContent(normalizedContent);

        List<ExpectationScriptModel> expectationScriptList = new ArrayList<>(1);
        expectationScriptList.add(expectationScript);

        ReplayActionItem actionItem = new ReplayActionItem();
        actionItem.setExpectationScriptList(expectationScriptList);

        ReplayActionCaseItem actionCaseItem = new ReplayActionCaseItem();
        actionCaseItem.setId("test-case-id");
        actionCaseItem.setParent(actionItem);
        ReplayCaseContext replayCaseContext = new ReplayCaseContext();
        List<ReplayCaseResult> replayCaseResultList = new ArrayList<>(1);

        ReplayCaseResult soaProviderReplayCaseResult = new ReplayCaseResult();

        List<ReplayCaseItem> helloServiceReplayCaseItemList = new ArrayList<>(1);
        ReplayCaseItem helloServiceProvider = new ReplayCaseItem();
        helloServiceProvider.setOperationName("HelloService");
        helloServiceProvider.setResponse("{\"order\":{\"id\":\"mark3\"}}");
        helloServiceReplayCaseItemList.add(helloServiceProvider);

        soaProviderReplayCaseResult.setCategoryName("SoaProvider");
        soaProviderReplayCaseResult.setReplayCaseList(helloServiceReplayCaseItemList);

        ReplayCaseResult soaConsumerReplayCaseResult = new ReplayCaseResult();

        List<ReplayCaseItem> serviceConsumerreplayCaseItemList = new ArrayList<>(2);
        ReplayCaseItem serviceConsumerA = new ReplayCaseItem();
        serviceConsumerA.setOperationName("HelloService.ConsumerA");
        serviceConsumerA.setRequest("{\"name\":\"serviceConsumerA\"}");
        serviceConsumerreplayCaseItemList.add(serviceConsumerA);

        ReplayCaseItem serviceConsumerB = new ReplayCaseItem();
        serviceConsumerB.setOperationName("HelloService.ConsumerB");
        serviceConsumerB.setRequest("{\"name\":\"serviceConsumerB\"}");
        serviceConsumerreplayCaseItemList.add(serviceConsumerB);

        soaConsumerReplayCaseResult.setCategoryName("SoaConsumer");
        soaConsumerReplayCaseResult.setReplayCaseList(serviceConsumerreplayCaseItemList);


        replayCaseResultList.add(soaProviderReplayCaseResult);
        replayCaseResultList.add(soaConsumerReplayCaseResult);
        replayCaseContext.setReplayCaseResultList(replayCaseResultList);
        actionCaseItem.setReplayCaseContext(replayCaseContext);
        handler.handle(actionCaseItem);
    }
}

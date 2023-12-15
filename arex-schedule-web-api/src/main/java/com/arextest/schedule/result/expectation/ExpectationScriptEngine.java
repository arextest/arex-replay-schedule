package com.arextest.schedule.result.expectation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExpectationScriptEngine {
    private ScriptEngine engine;

    public ExpectationScriptEngine(ExpectationService expectationService) {
        try {
            engine = new ScriptEngineManager().getEngineByName("javascript");
            String expectationScript = readFromInputStream(ExpectationScriptEngine.class.getResourceAsStream(
                "/arex/arex-expectation.js"));
            engine.eval(expectationScript);
            engine.put("expectationService", expectationService);
        } catch (Exception e) {
            LOGGER.error("Failed to init expectation script engine", e);
        }
    }

    private String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

    public void eval(String script) {
        try {
            engine.eval(script);
        } catch (Exception e) {
            LOGGER.error("Failed to eval expectation script: {}", script, e);
        }
    }
}

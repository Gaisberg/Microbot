package net.runelite.client.plugins.microbot.geflipper.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.geflipper.model.*;

import javax.inject.Singleton;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
public class JavaScriptEvaluator {
    @Getter
    private final ScriptEngine engine;
    private final ConcurrentHashMap<String, String> validationCache = new ConcurrentHashMap<>();

    public JavaScriptEvaluator() {
        this.engine = initializeEngine();
        if (engine != null) {
            log.info("JavaScript engine initialized: {}", engine.getClass().getSimpleName());
        }
        log.info("No engine available, use JDK 11 for JavaScript support.");
    }

    private ScriptEngine initializeEngine() {
        List<String> engineNames = Arrays.asList("nashorn", "js", "javascript");
        ScriptEngineManager manager = new ScriptEngineManager();
        for (String engineName : engineNames) {
            try {
                ScriptEngine engine = manager.getEngineByName(engineName);
                if (engine != null) {
                    return engine;
                }
            } catch (Exception e) {
                // Continue to the next engine
            }
        }
        return null;
    }

    public boolean evaluate(String jsCode, FlipOpportunity opportunity) {
        if (engine == null) {
            log.info("No JavaScript engine available");
            return false;
        }
        if (jsCode == null || jsCode.trim().isEmpty()) {
            return true; // Default to accepting if no code provided
        }

        try {
            engine.put("opportunity", opportunity);

            Object result = engine.eval(jsCode);

            return (boolean) result;

        } catch (ScriptException e) {
            log.error("Error evaluating JavaScript code: {}", e.getMessage());
            return false; // Default to rejecting on error
        }
    }

    public String validateCode(String jsCode) {
        if (engine == null) {
            log.info("No JavaScript engine available");
            return "No engine available, use JDK 11 for JavaScript support.";
        }
        if (jsCode == null || jsCode.trim().isEmpty()) {
            return null; // Empty code is valid
        }

        // Check cache first
        String cached = validationCache.get(jsCode);
        if (cached != null) {
            return cached.equals("VALID") ? null : cached;
        }

        try {
            // Create a test opportunity for validation
            FlipOpportunity testOpportunity = createTestOpportunity();

            // Only set the opportunity object
            engine.put("opportunity", testOpportunity);

            engine.eval(jsCode);

            validationCache.put(jsCode, "VALID");
            return null; // No errors

        } catch (ScriptException e) {
            String error = "JavaScript Error: " + e.getMessage();
            validationCache.put(jsCode, error);
            return error;
        } catch (Exception e) {
            String error = "Validation Error: " + e.getMessage();
            validationCache.put(jsCode, error);
            return error;
        }
    }

    private FlipOpportunity createTestOpportunity() {
        WikiMappingResponse testMapping = new WikiMappingResponse();
        testMapping.setId(1337);
        testMapping.setName("LEET");
        testMapping.setMembers(true);
        FlipOpportunity testOpportunity = new FlipOpportunity(testMapping);
        WikiLatestResponse.PriceEntry priceEntry = new WikiLatestResponse.PriceEntry();
        priceEntry.setHigh(1337);
        priceEntry.setLow(1337);
        WikiHistoryResponse.PriceEntry historyEntry = new WikiHistoryResponse.PriceEntry();
        historyEntry.setAvgHighPrice(1337);
        historyEntry.setAvgLowPrice(1337);
        historyEntry.setHighPriceVolume(1337);
        historyEntry.setLowPriceVolume(1337);

        testOpportunity.setLatest(priceEntry);
        testOpportunity.setHistoricalData(TimeFrame.FIVE_MINUTES, historyEntry);
        return new FlipOpportunity(testMapping);
    }

    public void clearCache() {
        validationCache.clear();
    }
}

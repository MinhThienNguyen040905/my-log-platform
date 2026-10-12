package com.mylog.safety.infrastructure;

import com.mylog.safety.application.RiskLevel;
import com.mylog.safety.application.SafetyScreeningService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GroqRiskClassifierTest {
    @Test void classifiesWithGroqAndRespectsConsent() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ObjectMapper mapper = new ObjectMapper();
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> level = new AtomicReference<>("MODERATE");
        server.createContext("/openai/v1/chat/completions", exchange -> {
            calls.incrementAndGet();
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] reply = mapper.writeValueAsString(Map.of("choices", List.of(Map.of(
                    "finish_reason", "stop", "message", Map.of("content", "{\"level\":\"" + level.get() + "\"}")))))
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, reply.length);
            exchange.getResponseBody().write(reply);
            exchange.close();
        });
        server.start();
        try {
            var classifier = new GroqRiskClassifier(endpoint(server), "openai/gpt-oss-20b", "test-key", 2000, mapper);
            var gate = new BaselineSafetyConfiguration().mvpSafetyPolicyGate("groq", "openai/gpt-oss-20b");
            var screening = new SafetyScreeningService(classifier, gate, Clock.systemUTC());
            assertEquals("FAIL_SAFE", screening.screen("Synthetic ambiguous phrase", false).decision());
            assertEquals(0, calls.get());
            var decision = screening.screen("Synthetic ambiguous phrase", true);
            assertEquals(RiskLevel.MODERATE, decision.riskLevel());
            assertEquals("CONSTRAIN", decision.decision());
            assertEquals("mvp-groq-safety-v1", decision.policyVersion());
            assertEquals(1, calls.get());
            level.set("LOW");
            assertEquals("ALLOW", screening.screen("Synthetic calm day", true).decision());
            assertEquals("SAFETY_FLOW", screening.screen("I want to die", true).decision());
            assertEquals(2, calls.get());
            var request = mapper.readTree(requestBody.get());
            assertEquals("json_schema", request.path("response_format").path("type").asText());
            assertFalse(request.path("store").asBoolean());
            assertTrue(requestBody.get().contains("Synthetic calm day"));
        } finally {
            server.stop(0);
        }
    }

    @Test void invalidOutputFailsClosedAndKeyStaysOnGroqHost() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/openai/v1/chat/completions", exchange -> {
            byte[] reply = "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"{\\\"level\\\":\\\"LOW\\\",\\\"extra\\\":true}\"}}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, reply.length);
            exchange.getResponseBody().write(reply);
            exchange.close();
        });
        server.start();
        try {
            var classifier = new GroqRiskClassifier(endpoint(server), "model", "test-key", 2000, new ObjectMapper());
            assertTrue(classifier.classify("Synthetic text").isEmpty());
        } finally {
            server.stop(0);
        }
        assertThrows(IllegalArgumentException.class, () -> new GroqRiskClassifier(
                "https://example.com/openai/v1/chat/completions", "model", "key", 2000, new ObjectMapper()));
    }

    private static String endpoint(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/openai/v1/chat/completions";
    }
}

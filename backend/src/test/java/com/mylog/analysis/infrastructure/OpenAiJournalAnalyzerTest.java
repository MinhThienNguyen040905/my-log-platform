package com.mylog.analysis.infrastructure;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiJournalAnalyzerTest {
    @Test void sendsMinimalJournalAndParsesStructuredResponse() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        ObjectMapper mapper = new ObjectMapper();
        server.createContext("/v1/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String content = """
                    {"sentiment":"NEUTRAL","sentimentScore":0.5,"emotions":[{"code":"CALM","score":0.5}],
                     "topics":[{"code":"OTHER","score":0.5}],"reflection":"Bạn có thể suy ngẫm thêm."}
                    """;
            byte[] result = mapper.writeValueAsString(java.util.Map.of(
                    "model", "test-model",
                    "choices", java.util.List.of(java.util.Map.of("finish_reason", "stop",
                            "message", java.util.Map.of("content", content))),
                    "usage", java.util.Map.of("prompt_tokens", 10, "completion_tokens", 20)))
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, result.length);
            exchange.getResponseBody().write(result);
            exchange.close();
        });
        server.start();
        try {
            var analyzer = new OpenAiJournalAnalyzer(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions",
                    "test-model", "test-key", 2000, BigDecimal.ONE, BigDecimal.ONE, mapper);
            var result = analyzer.analyze("Synthetic title", "Synthetic text");
            assertEquals("NEUTRAL", result.sentiment());
            assertEquals(10, result.inputTokens());
            assertEquals(new BigDecimal("0.000030"), result.estimatedCostUsd());
            assertEquals("Bearer test-key", authorization.get());
            var request = mapper.readTree(body.get());
            assertFalse(request.get("store").asBoolean());
            assertEquals("json_schema", request.get("response_format").get("type").asText());
            assertTrue(body.get().contains("Synthetic text"));
            assertFalse(body.get().contains("userId"));
        } finally {
            server.stop(0);
        }
    }

    @Test void rejectsNonTlsRemoteEndpoint() {
        assertThrows(IllegalArgumentException.class, () -> new OpenAiJournalAnalyzer(
                "http://example.com/v1/chat/completions", "model", "key", 2000,
                BigDecimal.ONE, BigDecimal.ONE, new ObjectMapper()));
    }
}

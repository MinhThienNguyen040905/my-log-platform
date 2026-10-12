package com.mylog.journal.infrastructure;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GroqWritingSuggestionGeneratorTest {
    @Test void sendsDraftOnlyToConfiguredEndpointAndValidatesQuestion() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> auth = new AtomicReference<>();
        ObjectMapper mapper = new ObjectMapper();
        server.createContext("/openai/v1/chat/completions", exchange -> {
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = mapper.writeValueAsString(Map.of("choices", java.util.List.of(Map.of(
                    "finish_reason", "stop", "message", Map.of("content",
                            "{\"question\":\"Bạn muốn kể thêm điều gì về khoảnh khắc đó?\"}")))))
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var generator = new GroqWritingSuggestionGenerator(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/openai/v1/chat/completions",
                    "openai/gpt-oss-20b", "test-key", 2000, mapper);
            var result = generator.generate("Synthetic draft");
            assertEquals("Bạn muốn kể thêm điều gì về khoảnh khắc đó?", result.question());
            assertEquals("writing-groq-v1", result.promptVersion());
            assertEquals("Bearer test-key", auth.get());
            var request = mapper.readTree(body.get());
            assertEquals("openai/gpt-oss-20b", request.get("model").asText());
            assertEquals("json_schema", request.get("response_format").get("type").asText());
            assertFalse(request.get("store").asBoolean());
            assertTrue(body.get().contains("Synthetic draft"));
            assertFalse(body.get().contains("userId"));
        } finally {
            server.stop(0);
        }
    }

    @Test void rejectsRemoteHttp() {
        assertThrows(IllegalArgumentException.class, () -> new GroqWritingSuggestionGenerator(
                "http://example.com/openai/v1/chat/completions", "model", "key", 2000,
                new ObjectMapper()));
    }

    @Test void groqKeyCannotBeSentToAnotherRemoteHost() {
        assertThrows(IllegalArgumentException.class, () -> new GroqWritingSuggestionGenerator(
                "https://api.openai.com/v1/chat/completions", "openai/gpt-oss-20b", "key", 2000,
                new ObjectMapper()));
    }

    @Test void configurationRequiresGroqAndBothApprovals() {
        var configuration = new WritingSuggestionGeneratorConfiguration();
        ObjectMapper mapper = new ObjectMapper();
        assertInstanceOf(TemplateWritingSuggestionGenerator.class,
                configuration.writingSuggestionGenerator(false, "none", "", "", "", 30000,
                        false, false, false, mapper));
        assertThrows(IllegalArgumentException.class, () -> configuration.writingSuggestionGenerator(
                true, "openai", "", "openai/gpt-oss-20b", "test-key", 30000,
                true, true, false, mapper));
        assertThrows(IllegalArgumentException.class, () -> configuration.writingSuggestionGenerator(
                true, "groq", "", "openai/gpt-oss-20b", "test-key", 30000,
                false, true, false, mapper));
        assertInstanceOf(GroqWritingSuggestionGenerator.class,
                configuration.writingSuggestionGenerator(true, "groq", "", "openai/gpt-oss-20b",
                        "test-key", 30000, true, true, false, mapper));
        assertInstanceOf(GroqWritingSuggestionGenerator.class,
                configuration.writingSuggestionGenerator(true, "groq", "", "openai/gpt-oss-20b",
                        "test-key", 30000, false, false, true, mapper));
    }
}

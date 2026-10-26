package com.mylog.knowledge.infrastructure.embedding;

import com.mylog.knowledge.application.EmbeddingProvider;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiEmbeddingProviderTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void embedsDocumentsWithGeminiTaskTypeAndNormalizesVectors() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> apiKey = new AtomicReference<>();
        server.createContext("/v1beta/models/gemini-embedding-001:batchEmbedContents", exchange -> {
            apiKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            List<Double> values = new ArrayList<>();
            values.add(3.0);
            values.add(4.0);
            for (int i = 2; i < 768; i++) values.add(0.0);
            byte[] response = mapper.writeValueAsBytes(Map.of(
                    "embeddings", List.of(Map.of("values", values))));
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var provider = provider(server);
            var result = provider.embedDocuments(List.of(
                    EmbeddingProvider.Input.document("Synthetic title", "Synthetic knowledge")));

            assertEquals("GEMINI", result.provider());
            assertEquals("gemini-embedding-001", result.model());
            assertEquals(768, result.dimensions());
            assertEquals(0.6f, result.vectors().getFirst()[0], 0.0001f);
            assertEquals(0.8f, result.vectors().getFirst()[1], 0.0001f);
            assertEquals("test-key", apiKey.get());
            var request = mapper.readTree(body.get()).get("requests").get(0);
            var config = request.get("embedContentConfig");
            assertEquals("RETRIEVAL_DOCUMENT", config.get("taskType").asText());
            assertEquals(768, config.get("outputDimensionality").asInt());
            assertEquals("Synthetic title", config.get("title").asText());
        } finally {
            server.stop(0);
        }
    }

    @Test void embedsQueryWithTheSameConfiguredModel() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> body = new AtomicReference<>();
        server.createContext("/v1beta/models/gemini-embedding-001:batchEmbedContents", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            List<Double> values = new ArrayList<>();
            values.add(1.0);
            for (int i = 1; i < 768; i++) values.add(0.0);
            byte[] response = mapper.writeValueAsBytes(Map.of(
                    "embeddings", List.of(Map.of("values", values))));
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            float[] vector = provider(server).embedQuery("Synthetic query");
            assertEquals(768, vector.length);
            var request = mapper.readTree(body.get()).get("requests").get(0);
            assertEquals("models/gemini-embedding-001", request.get("model").asText());
            var config = request.get("embedContentConfig");
            assertEquals("RETRIEVAL_QUERY", config.get("taskType").asText());
            assertTrue(config.get("title") == null);
        } finally {
            server.stop(0);
        }
    }

    @Test void rejectsNonTlsRemoteEndpoint() {
        var properties = new EmbeddingProperties(true, "gemini", "http://example.com/v1beta", "key",
                "gemini-embedding-001", "gemini-embedding-001", 768, 32, 30000, true);
        assertThrows(IllegalArgumentException.class,
                () -> new GeminiEmbeddingProvider(properties, mapper));
    }

    private GeminiEmbeddingProvider provider(HttpServer server) {
        var properties = new EmbeddingProperties(true, "gemini",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta", "test-key",
                "gemini-embedding-001", "gemini-embedding-001", 768, 32, 30000, true);
        return new GeminiEmbeddingProvider(properties, mapper);
    }
}

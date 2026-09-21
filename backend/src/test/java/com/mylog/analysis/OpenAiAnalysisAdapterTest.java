package com.mylog.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mylog.analysis.application.AiAnalysisInput;
import com.mylog.analysis.application.AiAnalysisOutput;
import com.mylog.analysis.application.AiProviderException;
import com.mylog.analysis.configuration.AiProperties;
import com.mylog.analysis.infrastructure.provider.OpenAiAnalysisAdapter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class OpenAiAnalysisAdapterTest {

    private final ObjectMapper mapper = JsonMapper.builder().build();
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void mapsSuccessfulStructuredResponse() {
        server.createContext("/success", exchange -> {
            String structured = mapper.writeValueAsString(Map.of(
                    "schemaVersion", "1.0",
                    "sentiment", "NEUTRAL",
                    "riskLevel", "NORMAL",
                    "emotions", List.of(Map.of("type", "CALM", "score", 0.75)),
                    "topics", List.of(Map.of("name", "work", "confidence", 0.8)),
                    "summary", "Summary",
                    "explanation", "Explanation",
                    "reflections", List.of("What mattered today?")));
            respond(exchange, 200, mapper.writeValueAsString(Map.of(
                    "output", List.of(Map.of("content", List.of(Map.of("text", structured)))),
                    "usage", Map.of("input_tokens", 21, "output_tokens", 13))));
        });

        AiAnalysisOutput output = adapter("/success", Duration.ofSeconds(2)).analyze(input());

        assertThat(output.sentiment()).isEqualTo("NEUTRAL");
        assertThat(output.inputTokens()).isEqualTo(21);
        assertThat(output.outputTokens()).isEqualTo(13);
        assertThat(output.provider()).isEqualTo("openai");
    }

    @Test
    void timeoutIsRetryable() {
        server.createContext("/timeout", exchange -> {
            try {
                Thread.sleep(500);
                respond(exchange, 200, "{}");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });

        assertProviderError("/timeout", Duration.ofMillis(100), "AI_TIMEOUT", true);
    }

    @Test
    void rateLimitAndServerErrorsAreRetryable() {
        server.createContext("/rate", exchange -> respond(exchange, 429, "{}"));
        server.createContext("/server", exchange -> respond(exchange, 503, "{}"));

        assertProviderError("/rate", Duration.ofSeconds(1), "AI_HTTP_429", true);
        assertProviderError("/server", Duration.ofSeconds(1), "AI_HTTP_503", true);
    }

    @Test
    void malformedJsonIsNotRetried() {
        server.createContext("/malformed", exchange -> respond(exchange, 200, "{not-json"));

        assertProviderError("/malformed", Duration.ofSeconds(1), "AI_MALFORMED_RESPONSE", false);
    }

    private void assertProviderError(String path, Duration timeout, String code, boolean retryable) {
        assertThatThrownBy(() -> adapter(path, timeout).analyze(input()))
                .isInstanceOf(AiProviderException.class)
                .satisfies(error -> {
                    AiProviderException providerError = (AiProviderException) error;
                    assertThat(providerError.code()).isEqualTo(code);
                    assertThat(providerError.retryable()).isEqualTo(retryable);
                });
    }

    private OpenAiAnalysisAdapter adapter(String path, Duration timeout) {
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
        AiProperties properties = new AiProperties(
                "openai", "test-key", endpoint, "test-model", "analysis-v1", "1.0",
                false, false, 8, 4, Duration.ofSeconds(1), Duration.ofMinutes(2),
                Duration.ofSeconds(1), timeout, Duration.ofSeconds(1), Duration.ofMinutes(1),
                5, Duration.ofSeconds(30), 3);
        return new OpenAiAnalysisAdapter(properties, mapper);
    }

    private AiAnalysisInput input() {
        return new AiAnalysisInput(UUID.randomUUID(), 1, "Private content", 5, null, null, 3);
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}

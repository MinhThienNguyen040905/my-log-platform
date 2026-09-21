package com.mylog.analysis.infrastructure.provider;

import com.mylog.analysis.application.AiAnalysisInput;
import com.mylog.analysis.application.AiAnalysisOutput;
import com.mylog.analysis.application.AiAnalysisPort;
import com.mylog.analysis.application.AiProviderException;
import com.mylog.analysis.configuration.AiProperties;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "mylog.ai", name = "provider", havingValue = "openai")
public class OpenAiAnalysisAdapter implements AiAnalysisPort {

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient client;

    public OpenAiAnalysisAdapter(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.client = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("AI_API_KEY is required when AI_PROVIDER=openai");
        }
    }

    @Override
    public AiAnalysisOutput analyze(AiAnalysisInput input) {
        long started = System.nanoTime();
        try {
            String requestBody = objectMapper.writeValueAsString(request(input));
            HttpRequest request = HttpRequest.newBuilder(properties.endpoint())
                    .timeout(properties.responseTimeout())
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429 || response.statusCode() >= 500) {
                throw new AiProviderException("AI_HTTP_" + response.statusCode(), "AI provider is temporarily unavailable", true);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiProviderException("AI_HTTP_" + response.statusCode(), "AI provider rejected the request", false);
            }
            JsonNode root = objectMapper.readTree(response.body());
            String text = extractOutputText(root);
            ProviderPayload payload = objectMapper.readValue(text, ProviderPayload.class);
            JsonNode usage = root.path("usage");
            return new AiAnalysisOutput(
                    payload.schemaVersion(), payload.sentiment(), payload.riskLevel(),
                    payload.emotions(), payload.topics(), payload.summary(), payload.explanation(),
                    payload.reflections(), "openai", properties.model(), properties.promptVersion(),
                    nullableInt(usage, "input_tokens"), nullableInt(usage, "output_tokens"),
                    (System.nanoTime() - started) / 1_000_000);
        } catch (java.net.http.HttpTimeoutException exception) {
            throw new AiProviderException("AI_TIMEOUT", "AI provider timed out", true, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("AI_INTERRUPTED", "AI provider call was interrupted", true, exception);
        } catch (AiProviderException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new AiProviderException("AI_MALFORMED_RESPONSE", "AI provider returned malformed JSON", false, exception);
        }
    }

    private Map<String, Object> request(AiAnalysisInput input) {
        Map<String, Object> emotionItem = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("type", "score"),
                "properties", Map.of(
                        "type", Map.of("type", "string", "enum", List.of(
                                "JOY", "SADNESS", "ANGER", "FEAR", "ANXIETY", "CALM", "HOPE",
                                "GRATITUDE", "LONELINESS", "FRUSTRATION", "EXCITEMENT")),
                        "score", Map.of("type", "number", "minimum", 0, "maximum", 1)));
        Map<String, Object> topicItem = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("name", "confidence"),
                "properties", Map.of(
                        "name", Map.of("type", "string", "maxLength", 100),
                        "confidence", Map.of("type", "number", "minimum", 0, "maximum", 1)));
        Map<String, Object> schema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("schemaVersion", "sentiment", "riskLevel", "emotions", "topics", "summary", "explanation", "reflections"),
                "properties", Map.of(
                        "schemaVersion", Map.of("type", "string", "const", properties.schemaVersion()),
                        "sentiment", Map.of("type", "string", "enum", List.of("POSITIVE", "NEUTRAL", "NEGATIVE")),
                        "riskLevel", Map.of("type", "string", "enum", List.of("NORMAL", "LOW", "MODERATE", "HIGH", "CRITICAL")),
                        "emotions", Map.of("type", "array", "items", emotionItem),
                        "topics", Map.of("type", "array", "items", topicItem),
                        "summary", Map.of("type", "string"),
                        "explanation", Map.of("type", "string"),
                        "reflections", Map.of("type", "array", "maxItems", properties.maxReflections(), "items", Map.of("type", "string"))));
        Map<String, Object> format = Map.of("type", "json_schema", "name", "mylog_analysis", "strict", true, "schema", schema);
        Map<String, Object> userPayload = new LinkedHashMap<>();
        userPayload.put("content", input.content());
        userPayload.put("moodScore", input.moodScore());
        userPayload.put("stressScore", input.stressScore());
        userPayload.put("energyScore", input.energyScore());
        return Map.of(
                "model", properties.model(),
                "instructions", "Analyze without diagnosis, medication advice, or therapy claims. Return only the requested JSON.",
                "input", List.of(Map.of("role", "user", "content", objectMapper.writeValueAsString(userPayload))),
                "text", Map.of("format", format));
    }

    private String extractOutputText(JsonNode root) {
        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (content.isArray()) {
                    for (JsonNode part : content) {
                        if (part.has("text") && !part.path("text").asText().isBlank()) {
                            return part.path("text").asText();
                        }
                    }
                }
            }
        }
        throw new AiProviderException("AI_MALFORMED_RESPONSE", "AI provider response has no structured output", false);
    }

    private Integer nullableInt(JsonNode node, String field) {
        return node.has(field) ? node.path(field).asInt() : null;
    }

    private record ProviderPayload(
            String schemaVersion,
            String sentiment,
            String riskLevel,
            List<AiAnalysisOutput.Emotion> emotions,
            List<AiAnalysisOutput.Topic> topics,
            String summary,
            String explanation,
            List<String> reflections) {}
}

package com.mylog.analysis.infrastructure;

import com.mylog.analysis.application.ThinkingPatternTaxonomy;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Adapter behaviour with a fake transport; no network. Synthetic text only. */
class OpenAiPatternExtractorTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<String> sent = new AtomicReference<>();

    @Test void sendsOnlyTitleAndTextWithAStrictSchemaAndParsesSignals() throws Exception {
        var extractor = extractor(200, envelope("stop", Map.of("signals", List.of(
                Map.of("code", "RUMINATION", "quote", "Cả tối mình cứ tua lại câu nói đó.", "confidence", 0.82)))));
        var result = extractor.extract("Tối nay", "Cả tối mình cứ tua lại câu nói đó.");

        assertTrue(result.succeeded());
        assertEquals(1, result.signals().size());
        assertEquals("RUMINATION", result.signals().getFirst().code());
        assertEquals(0, new BigDecimal("0.82").compareTo(result.signals().getFirst().confidence()));
        assertEquals(120, result.inputTokens());
        assertEquals(OpenAiPatternExtractor.PROMPT_VERSION, result.version());

        JsonNode body = mapper.readTree(sent.get());
        assertFalse(body.path("store").asBoolean(true), "store=false");
        JsonNode format = body.path("response_format").path("json_schema");
        assertTrue(format.path("strict").asBoolean());
        assertEquals(ThinkingPatternTaxonomy.CODES.size(),
                format.path("schema").path("properties").path("signals").path("items").path("properties")
                        .path("code").path("enum").size());
        JsonNode user = mapper.readTree(body.path("messages").path(1).path("content").asText());
        assertEquals(List.of("journalText", "title"), iterate(user));
    }

    @Test void providerProblemsBecomeAFailedResult() {
        assertFalse(extractor(500, "{}").extract("t", "text").succeeded());
        assertFalse(extractor(200, envelope("length", Map.of("signals", List.of()))).extract("t", "text").succeeded());
        assertFalse(extractor(200, "not json").extract("t", "text").succeeded());
        assertFalse(extractor(200, envelope("stop", Map.of("signals", List.of(Map.of("code", 7)))))
                .extract("t", "text").succeeded());
        String refusal = "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"refusal\":\"no\",\"content\":null}}]}";
        assertFalse(extractor(200, refusal).extract("t", "text").succeeded());
        var throwing = new OpenAiPatternExtractor("https://api.example.test/v1/chat/completions", "test-model", "key",
                5000, mapper, BigDecimal.ZERO, BigDecimal.ZERO, (u, k, t, b) -> { throw new java.io.IOException("down"); });
        assertFalse(throwing.extract("t", "text").succeeded());
    }

    @Test void rejectsUnsafeConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new OpenAiPatternExtractor("http://api.example.test/v1",
                "m", "key", 5000, mapper, BigDecimal.ZERO, BigDecimal.ZERO, (u, k, t, b) -> null));
        assertThrows(IllegalArgumentException.class, () -> new OpenAiPatternExtractor("https://api.example.test/v1",
                "m", " ", 5000, mapper, BigDecimal.ZERO, BigDecimal.ZERO, (u, k, t, b) -> null));
    }

    private OpenAiPatternExtractor extractor(int status, String responseBody) {
        return new OpenAiPatternExtractor("https://api.example.test/v1/chat/completions", "test-model", "key", 5000,
                mapper, new BigDecimal("0.15"), new BigDecimal("0.60"), (uri, key, timeout, json) -> {
                    sent.set(json);
                    return new OpenAiPatternExtractor.HttpTransport.Response(status, responseBody);
                });
    }

    private String envelope(String finishReason, Object content) {
        return mapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of("finish_reason", finishReason,
                        "message", Map.of("content", mapper.writeValueAsString(content)))),
                "usage", Map.of("prompt_tokens", 120, "completion_tokens", 30)));
    }

    private static List<String> iterate(JsonNode node) {
        List<String> names = new java.util.ArrayList<>();
        node.propertyNames().forEach(names::add);
        return names.stream().sorted().toList();
    }
}

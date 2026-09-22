package com.mylog.analysis.service;

import com.mylog.analysis.config.AiProperties;
import com.mylog.analysis.port.AiAnalysisOutput;
import com.mylog.analysis.port.AiProviderException;
import java.util.Locale;
import java.util.Set;
import java.text.Normalizer;
import org.springframework.stereotype.Component;

@Component
public class AiOutputValidator {

    private static final Set<String> SENTIMENTS = Set.of("POSITIVE", "NEUTRAL", "NEGATIVE");
    private static final Set<String> RISKS = Set.of("NORMAL", "LOW", "MODERATE", "HIGH", "CRITICAL");
    private static final Set<String> EMOTIONS = Set.of(
            "JOY", "SADNESS", "ANGER", "FEAR", "ANXIETY", "CALM", "HOPE",
            "GRATITUDE", "LONELINESS", "FRUSTRATION", "EXCITEMENT");
    private static final Set<String> PROHIBITED_CLINICAL_PHRASES = Set.of(
            "you have depression", "you are depressed", "diagnosed with",
            "take medication", "stop medication", "increase your dose",
            "this is therapy", "i am your therapist", "ban bi tram cam",
            "ban duoc chan doan", "hay dung thuoc", "ngung thuoc", "tang lieu",
            "toi la nha tri lieu", "day la tri lieu");

    private final AiProperties properties;

    public AiOutputValidator(AiProperties properties) {
        this.properties = properties;
    }

    public AiAnalysisOutput validate(AiAnalysisOutput output) {
        if (output == null || !properties.schemaVersion().equals(output.schemaVersion())) {
            throw invalid("AI output schema version is invalid");
        }
        if (!SENTIMENTS.contains(output.sentiment()) || !RISKS.contains(output.riskLevel())) {
            throw invalid("AI output contains an unsupported classification");
        }
        if (output.emotions() == null || output.topics() == null || output.reflections() == null
                || output.emotions().stream().anyMatch(item -> item == null
                        || !EMOTIONS.contains(item.type()) || item.score() == null
                        || item.score().signum() < 0 || item.score().compareTo(java.math.BigDecimal.ONE) > 0)
                || output.topics().stream().anyMatch(item -> item == null
                        || item.name() == null || item.name().isBlank() || item.name().length() > 100
                        || item.confidence() == null || item.confidence().signum() < 0
                        || item.confidence().compareTo(java.math.BigDecimal.ONE) > 0)
                || output.reflections().size() > properties.maxReflections()
                || output.reflections().stream().anyMatch(value -> value == null || value.isBlank())
                || output.emotions().stream().map(AiAnalysisOutput.Emotion::type).distinct().count() != output.emotions().size()
                || normalizedTopicCount(output) != output.topics().size()) {
            throw invalid("AI output does not match the required structure");
        }
        String generatedText = normalize(String.join(" ", nullToEmpty(output.summary()),
                nullToEmpty(output.explanation()), String.join(" ", output.reflections())));
        if (PROHIBITED_CLINICAL_PHRASES.stream().anyMatch(generatedText::contains)) {
            throw new AiProviderException("AI_UNSAFE_OUTPUT", "AI output violated the clinical safety policy", false);
        }
        return output;
    }

    private AiProviderException invalid(String message) {
        return new AiProviderException("AI_INVALID_OUTPUT", message, false);
    }

    private String nullToEmpty(String value) { return value == null ? "" : value; }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private long normalizedTopicCount(AiAnalysisOutput output) {
        return output.topics().stream()
                .map(topic -> topic.name().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " "))
                .distinct().count();
    }
}

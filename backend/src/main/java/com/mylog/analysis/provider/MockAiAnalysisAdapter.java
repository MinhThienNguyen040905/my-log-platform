package com.mylog.analysis.provider;

import com.mylog.analysis.config.AiProperties;
import com.mylog.analysis.port.AiAnalysisInput;
import com.mylog.analysis.port.AiAnalysisOutput;
import com.mylog.analysis.port.AiAnalysisPort;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "mylog.ai", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockAiAnalysisAdapter implements AiAnalysisPort {

    private final AiProperties properties;

    public MockAiAnalysisAdapter(AiProperties properties) {
        this.properties = properties;
    }

    @Override
    public AiAnalysisOutput analyze(AiAnalysisInput input) {
        long started = System.nanoTime();
        String sentiment = input.moodScore() >= 7 ? "POSITIVE" : input.moodScore() <= 4 ? "NEGATIVE" : "NEUTRAL";
        String emotion = input.moodScore() >= 7 ? "JOY" : input.moodScore() <= 4 ? "SADNESS" : "CALM";
        return new AiAnalysisOutput(
                properties.schemaVersion(), sentiment, "NORMAL",
                List.of(new AiAnalysisOutput.Emotion(emotion, new BigDecimal("0.8000"))),
                List.of(new AiAnalysisOutput.Topic("personal-reflection", new BigDecimal("0.7000"))),
                "A private reflection was analyzed.",
                "This result describes patterns in the entry and is not a clinical diagnosis.",
                List.of("What part of this experience would you like to understand better?"),
                "mock", properties.model(), properties.promptVersion(), 0, 0,
                (System.nanoTime() - started) / 1_000_000);
    }
}

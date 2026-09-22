package com.mylog.analysis;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mylog.analysis.provider.AiGateway;
import com.mylog.analysis.config.AiProperties;
import com.mylog.analysis.port.AiAnalysisInput;
import com.mylog.analysis.port.AiAnalysisPort;
import com.mylog.analysis.port.AiProviderException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiGatewayTest {

    @Test
    void circuitOpensAfterConfiguredTransientFailures() {
        AiAnalysisPort failing = input -> {
            throw new AiProviderException("AI_TIMEOUT", "timeout", true);
        };
        AiProperties properties = properties(2);
        AiGateway gateway = new AiGateway(
                failing, properties, Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"), ZoneOffset.UTC),
                new SimpleMeterRegistry());
        AiAnalysisInput input = new AiAnalysisInput(UUID.randomUUID(), 1, "content", 5, null, null, 3);

        assertThatThrownBy(() -> gateway.analyze(input))
                .isInstanceOf(AiProviderException.class)
                .extracting(error -> ((AiProviderException) error).code()).isEqualTo("AI_TIMEOUT");
        assertThatThrownBy(() -> gateway.analyze(input)).isInstanceOf(AiProviderException.class);
        assertThatThrownBy(() -> gateway.analyze(input))
                .isInstanceOf(AiProviderException.class)
                .extracting(error -> ((AiProviderException) error).code()).isEqualTo("AI_CIRCUIT_OPEN");
    }

    static AiProperties properties(int threshold) {
        return new AiProperties(
                "mock", "", URI.create("http://localhost"), "test-model", "analysis-v1", "1.0",
                false, false, 8, 4, Duration.ofSeconds(1), Duration.ofMinutes(2),
                Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1),
                Duration.ofMinutes(1), threshold, Duration.ofSeconds(30), 3);
    }
}

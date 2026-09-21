package com.mylog.analysis.application;

import com.mylog.analysis.configuration.AiProperties;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class AiGateway {

    private final AiAnalysisPort provider;
    private final AiProperties properties;
    private final Clock clock;
    private final MeterRegistry metrics;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicReference<Instant> openUntil = new AtomicReference<>();

    public AiGateway(AiAnalysisPort provider, AiProperties properties, Clock clock, MeterRegistry metrics) {
        this.provider = provider;
        this.properties = properties;
        this.clock = clock;
        this.metrics = metrics;
    }

    public AiAnalysisOutput analyze(AiAnalysisInput input) {
        Instant now = clock.instant();
        Instant until = openUntil.get();
        if (until != null && now.isBefore(until)) {
            metrics.counter("ai.provider.requests", "outcome", "circuit_open").increment();
            throw new AiProviderException("AI_CIRCUIT_OPEN", "AI provider circuit is open", true);
        }
        if (until != null) {
            openUntil.compareAndSet(until, null);
        }
        try {
            AiAnalysisOutput output = provider.analyze(input);
            consecutiveFailures.set(0);
            metrics.counter("ai.provider.requests", "outcome", "success").increment();
            return output;
        } catch (AiProviderException exception) {
            metrics.counter("ai.provider.requests", "outcome", exception.retryable() ? "transient_error" : "invalid").increment();
            if (exception.retryable() && consecutiveFailures.incrementAndGet() >= properties.circuitFailureThreshold()) {
                openUntil.set(now.plus(properties.circuitOpenDuration()));
                consecutiveFailures.set(0);
            }
            throw exception;
        }
    }
}

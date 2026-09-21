package com.mylog.shared.api;

import com.mylog.shared.web.RequestCorrelationFilter;
import io.micrometer.tracing.Tracer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorResponseFactory {

    private final Clock clock;
    private final Tracer tracer;

    public ApiErrorResponseFactory(Clock clock, ObjectProvider<Tracer> tracerProvider) {
        this.clock = clock;
        this.tracer = tracerProvider.getIfAvailable();
    }

    public ApiErrorResponse create(String code, String message) {
        return create(code, message, List.of());
    }

    public ApiErrorResponse create(
            String code, String message, List<FieldViolation> fieldViolations) {
        return new ApiErrorResponse(
                code,
                message,
                currentTraceOrRequestId(),
                Instant.now(clock),
                fieldViolations);
    }

    private String currentTraceOrRequestId() {
        if (tracer != null && tracer.currentSpan() != null) {
            return tracer.currentSpan().context().traceId();
        }
        return MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID);
    }
}

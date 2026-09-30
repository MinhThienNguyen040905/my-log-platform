package com.mylog.platform.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
public final class ApiProblemFactory {
    private static final String PROBLEM_BASE_URI = "https://mylog.app/problems/";
    private static final String DEFAULT_DETAIL = "Yêu cầu không thể được xử lý.";

    private final Clock clock;

    public ApiProblemFactory(Clock clock) {
        this.clock = clock;
    }

    public ApiProblem create(ErrorCode errorCode, HttpServletRequest request) {
        return create(errorCode, DEFAULT_DETAIL, request, List.of());
    }

    public ApiProblem create(
            ErrorCode errorCode,
            String safeDetail,
            HttpServletRequest request,
            List<FieldViolation> errors
    ) {
        Object requestIdAttribute = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        String requestId = requestIdAttribute == null ? "unavailable" : requestIdAttribute.toString();

        return new ApiProblem(
                URI.create(PROBLEM_BASE_URI + errorCode.slug()),
                errorCode.title(),
                errorCode.status().value(),
                errorCode.name(),
                safeDetail,
                request.getRequestURI(),
                requestId,
                Instant.now(clock),
                errors
        );
    }
}

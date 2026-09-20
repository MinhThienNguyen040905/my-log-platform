package com.mylog.shared.exception;

import com.mylog.shared.api.ApiErrorResponse;
import com.mylog.shared.api.FieldViolation;
import io.micrometer.tracing.Tracer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;
    private final Tracer tracer;

    public GlobalExceptionHandler(Clock clock, ObjectProvider<Tracer> tracerProvider) {
        this.clock = clock;
        this.tracer = tracerProvider.getIfAvailable();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.code(), exception.getMessage(), List.of());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiErrorResponse> handleConflict(ConflictException exception) {
        return response(HttpStatus.CONFLICT, exception.code(), exception.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", violations);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unhandled backend error", exception);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                List.of());
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status, String code, String message, List<FieldViolation> violations) {
        ApiErrorResponse body = new ApiErrorResponse(
                code, message, currentTraceId(), Instant.now(clock), violations);
        return ResponseEntity.status(status).body(body);
    }

    private String currentTraceId() {
        return tracer == null || tracer.currentSpan() == null
                ? null
                : tracer.currentSpan().context().traceId();
    }
}

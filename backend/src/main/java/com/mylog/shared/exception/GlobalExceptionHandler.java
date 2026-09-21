package com.mylog.shared.exception;

import com.mylog.shared.api.ApiErrorResponse;
import com.mylog.shared.api.ApiErrorCodes;
import com.mylog.shared.api.ApiErrorResponseFactory;
import com.mylog.shared.api.FieldViolation;
import com.mylog.shared.logging.SensitiveDataSanitizer;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ApiErrorResponseFactory responseFactory;
    private final SensitiveDataSanitizer sanitizer;

    public GlobalExceptionHandler(
            ApiErrorResponseFactory responseFactory, SensitiveDataSanitizer sanitizer) {
        this.responseFactory = responseFactory;
        this.sanitizer = sanitizer;
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
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCodes.VALIDATION_FAILED,
                "Request validation failed",
                violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<FieldViolation> violations = exception.getConstraintViolations().stream()
                .map(violation -> new FieldViolation(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCodes.VALIDATION_FAILED,
                "Request validation failed",
                violations);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<ApiErrorResponse> handleInvalidRequest(Exception exception) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCodes.INVALID_REQUEST,
                "Request is malformed or contains invalid values",
                List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception) {
        return response(
                HttpStatus.METHOD_NOT_ALLOWED,
                ApiErrorCodes.METHOD_NOT_ALLOWED,
                "HTTP method is not supported for this resource",
                List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception) {
        return response(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                ApiErrorCodes.UNSUPPORTED_MEDIA_TYPE,
                "Content type is not supported",
                List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException exception) {
        return response(
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.RESOURCE_NOT_FOUND,
                "Resource does not exist",
                List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        log.error(
                "Unhandled backend error type={} message={}",
                exception.getClass().getName(),
                sanitizer.sanitize(exception.getMessage()));
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCodes.INTERNAL_ERROR,
                "An unexpected error occurred",
                List.of());
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status, String code, String message, List<FieldViolation> violations) {
        ApiErrorResponse body = responseFactory.create(code, message, violations);
        return ResponseEntity.status(status).body(body);
    }
}

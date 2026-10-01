package com.mylog.platform.web;

import com.mylog.platform.security.UnauthenticatedUserException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public final class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ApiProblemFactory problemFactory;

    public GlobalExceptionHandler(ApiProblemFactory problemFactory) {
        this.problemFactory = problemFactory;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiProblem> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldViolation::field))
                .toList();
        return response(ErrorCode.VALIDATION_ERROR, "Một hoặc nhiều trường không hợp lệ.", request, violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiProblem> handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        List<FieldViolation> violations = exception.getConstraintViolations().stream()
                .map(violation -> new FieldViolation(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .sorted(Comparator.comparing(FieldViolation::field))
                .toList();
        return response(ErrorCode.VALIDATION_ERROR, "Một hoặc nhiều trường không hợp lệ.", request, violations);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiProblem> handleMalformedRequest(Exception ignored, HttpServletRequest request) {
        return response(ErrorCode.MALFORMED_REQUEST, "Cấu trúc hoặc kiểu dữ liệu của yêu cầu không hợp lệ.", request, List.of());
    }

    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ApiProblem> handleApplicationException(ApplicationException exception, HttpServletRequest request) {
        return response(exception.errorCode(), exception.safeDetail(), request, List.of());
    }

    @ExceptionHandler(UnauthenticatedUserException.class)
    ResponseEntity<ApiProblem> handleUnauthenticated(UnauthenticatedUserException ignored, HttpServletRequest request) {
        return response(ErrorCode.UNAUTHORIZED, "Bạn cần đăng nhập để tiếp tục.", request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiProblem> handleAccessDenied(AccessDeniedException ignored, HttpServletRequest request) {
        return response(ErrorCode.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.", request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiProblem> handleNoResource(NoResourceFoundException ignored, HttpServletRequest request) {
        return response(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tài nguyên được yêu cầu.", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiProblem> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception type={} requestId={}",
                exception.getClass().getName(),
                request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
        return response(ErrorCode.INTERNAL_ERROR, "Hệ thống gặp lỗi không mong đợi.", request, List.of());
    }

    private ResponseEntity<ApiProblem> response(
            ErrorCode errorCode,
            String safeDetail,
            HttpServletRequest request,
            List<FieldViolation> violations
    ) {
        ApiProblem problem = problemFactory.create(errorCode, safeDetail, request, violations);
        return ResponseEntity.status(errorCode.status())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}

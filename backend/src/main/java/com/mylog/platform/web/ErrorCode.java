package com.mylog.platform.web;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "validation-error", "Dữ liệu không hợp lệ"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "malformed-request", "Yêu cầu không hợp lệ"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "unauthorized", "Cần đăng nhập"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "forbidden", "Không có quyền truy cập"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "resource-not-found", "Không tìm thấy tài nguyên"),
    CONFLICT(HttpStatus.CONFLICT, "conflict", "Dữ liệu đã thay đổi hoặc bị trùng"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "rate-limited", "Quá nhiều yêu cầu"),
    DEPENDENCY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "dependency-unavailable", "Dịch vụ tạm thời không khả dụng"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Hệ thống gặp lỗi không mong đợi");

    private final HttpStatus status;
    private final String slug;
    private final String title;

    ErrorCode(HttpStatus status, String slug, String title) {
        this.status = status;
        this.slug = slug;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String slug() {
        return slug;
    }

    public String title() {
        return title;
    }
}

package com.mylog.platform.security;

import com.mylog.platform.web.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
final class ProblemAccessDeniedHandler implements AccessDeniedHandler {
    private final SecurityProblemWriter writer;

    ProblemAccessDeniedHandler(SecurityProblemWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException ignored
    ) throws IOException, ServletException {
        writer.write(request, response, ErrorCode.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
    }
}

package com.mylog.platform.security;

import com.mylog.platform.web.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
final class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final SecurityProblemWriter writer;

    ProblemAuthenticationEntryPoint(SecurityProblemWriter writer) {
        this.writer = writer;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException ignored
    ) throws IOException, ServletException {
        writer.write(request, response, ErrorCode.UNAUTHORIZED, "Bạn cần đăng nhập để tiếp tục.");
    }
}

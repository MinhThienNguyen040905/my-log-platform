package com.mylog.platform.security;

import com.mylog.platform.web.ApiProblem;
import com.mylog.platform.web.ApiProblemFactory;
import com.mylog.platform.web.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
final class SecurityProblemWriter {
    private final ObjectMapper objectMapper;
    private final ApiProblemFactory problemFactory;

    SecurityProblemWriter(ObjectMapper objectMapper, ApiProblemFactory problemFactory) {
        this.objectMapper = objectMapper;
        this.problemFactory = problemFactory;
    }

    void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode, String detail)
            throws IOException {
        ApiProblem problem = problemFactory.create(errorCode, detail, request, java.util.List.of());
        response.setStatus(errorCode.status().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}

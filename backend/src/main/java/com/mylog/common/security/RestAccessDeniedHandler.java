package com.mylog.common.security;

import com.mylog.common.api.ApiErrorCodes;
import com.mylog.common.api.ApiErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiErrorResponseFactory responseFactory;
    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(
            ApiErrorResponseFactory responseFactory, ObjectMapper objectMapper) {
        this.responseFactory = responseFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                responseFactory.create(
                        ApiErrorCodes.ACCESS_DENIED,
                        "You do not have permission to access this resource"));
    }
}

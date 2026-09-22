package com.mylog.common.security;

import com.mylog.common.api.ApiErrorCodes;
import com.mylog.common.api.ApiErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApiErrorResponseFactory responseFactory;
    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(
            ApiErrorResponseFactory responseFactory, ObjectMapper objectMapper) {
        this.responseFactory = responseFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                responseFactory.create(
                        ApiErrorCodes.AUTHENTICATION_REQUIRED,
                        "Authentication is required to access this resource"));
    }
}

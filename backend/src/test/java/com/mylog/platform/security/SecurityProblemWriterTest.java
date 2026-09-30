package com.mylog.platform.security;

import com.mylog.platform.web.ApiProblemFactory;
import com.mylog.platform.web.ErrorCode;
import com.mylog.platform.web.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityProblemWriterTest {

    @Test
    void writesStableProblemJsonWithoutAuthenticationExceptionDetails() throws Exception {
        var objectMapper = JsonMapper.builder().findAndAddModules().build();
        var factory = new ApiProblemFactory(
                Clock.fixed(Instant.parse("2026-09-30T03:00:00Z"), ZoneOffset.UTC));
        var writer = new SecurityProblemWriter(objectMapper, factory);
        var request = new MockHttpServletRequest("GET", "/api/v1/private");
        request.setAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE, "request-12345");
        var response = new MockHttpServletResponse();

        writer.write(request, response, ErrorCode.UNAUTHORIZED, "Bạn cần đăng nhập để tiếp tục.");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString())
                .contains("\"code\":\"UNAUTHORIZED\"")
                .contains("\"requestId\":\"request-12345\"")
                .doesNotContain("password", "token", "stackTrace");
    }
}

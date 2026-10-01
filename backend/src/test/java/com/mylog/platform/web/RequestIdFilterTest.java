package com.mylog.platform.web;

import com.mylog.platform.id.IdGenerator;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {
    private static final UUID GENERATED_ID = UUID.fromString("019d0528-cc00-7000-8000-000000000001");
    private final IdGenerator idGenerator = () -> GENERATED_ID;
    private final RequestIdFilter filter = new RequestIdFilter(idGenerator);

    @Test
    void preservesSafeRequestIdAndAddsMdcContext() throws Exception {
        var request = new MockHttpServletRequest("GET", "/test");
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "client-request-123");
        request.addHeader("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            assertThat(MDC.get("requestId")).isEqualTo("client-request-123");
            assertThat(MDC.get("traceId")).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        });

        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("client-request-123");
        assertThat(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE)).isEqualTo("client-request-123");
        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void replacesUnsafeRequestIdInsteadOfReflectingIt() throws Exception {
        var request = new MockHttpServletRequest("GET", "/test");
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "unsafe\r\nheader");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
        });

        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo(GENERATED_ID.toString());
    }
}

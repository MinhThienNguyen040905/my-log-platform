package com.mylog.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void keepsAValidClientRequestIdDuringTheRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/system/info");
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "request-12345678");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] observedRequestId = new String[1];
        FilterChain chain = (ignoredRequest, ignoredResponse) ->
                observedRequestId[0] = MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID);

        filter.doFilter(request, response, chain);

        assertThat(observedRequestId[0]).isEqualTo("request-12345678");
        assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER))
                .isEqualTo("request-12345678");
        assertThat(MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID)).isNull();
    }

    @Test
    void replacesAnUnsafeClientRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "unsafe value\nheader");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

        assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER))
                .matches("[0-9a-f-]{36}")
                .doesNotContain("unsafe");
    }
}

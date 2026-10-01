package com.mylog.platform.web;

import com.mylog.platform.id.IdGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class RequestIdFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";

    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{7,127}");
    private static final Pattern TRACE_PARENT = Pattern.compile("^[\\da-fA-F]{2}-([\\da-fA-F]{32})-[\\da-fA-F]{16}-[\\da-fA-F]{2}$");

    private final IdGenerator idGenerator;

    public RequestIdFilter(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
        String traceId = resolveTraceId(request.getHeader("traceparent"), requestId);

        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put("requestId", requestId);
        MDC.put("traceId", traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
            MDC.remove("traceId");
        }
    }

    private String resolveRequestId(String candidate) {
        if (candidate != null && SAFE_REQUEST_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return idGenerator.next().toString();
    }

    private String resolveTraceId(String traceParent, String requestId) {
        if (traceParent != null) {
            Matcher matcher = TRACE_PARENT.matcher(traceParent);
            if (matcher.matches() && !matcher.group(1).chars().allMatch(character -> character == '0')) {
                return matcher.group(1).toLowerCase(Locale.ROOT);
            }
        }
        return requestId.replace("-", "");
    }
}

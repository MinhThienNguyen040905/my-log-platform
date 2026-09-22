package com.mylog.common.logging;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SensitiveDataSanitizer {

    private static final String REDACTED = "$1[REDACTED]";

    private static final List<Pattern> SENSITIVE_PATTERNS = List.of(
            Pattern.compile("(?i)(authorization\\s*[:=]\\s*bearer\\s+)[^\\s,;]+"),
            Pattern.compile("(?i)((?:password|passwd|pwd)\\s*[:=]\\s*)[^\\s,;]+"),
            Pattern.compile("(?i)((?:api[-_]?key|secret|token|cookie|set-cookie)\\s*[:=]\\s*)[^\\s,;]+"));

    public String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String sanitized = value;
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll(REDACTED);
        }
        return sanitized;
    }
}

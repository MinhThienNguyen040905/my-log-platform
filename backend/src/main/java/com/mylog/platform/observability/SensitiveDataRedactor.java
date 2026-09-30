package com.mylog.platform.observability;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Allow future structured logs to include metadata without leaking known sensitive fields. */
public final class SensitiveDataRedactor {
    public static final String REDACTED = "[REDACTED]";

    private static final Set<String> SENSITIVE_FRAGMENTS = Set.of(
            "authorization", "cookie", "password", "secret", "token", "email",
            "journal", "content", "prompt", "response", "location", "caption"
    );

    private SensitiveDataRedactor() {
    }

    public static Map<String, Object> redact(Map<String, ?> metadata) {
        var redacted = new LinkedHashMap<String, Object>();
        metadata.forEach((key, value) -> redacted.put(key, isSensitive(key) ? REDACTED : safeValue(value)));
        return Map.copyOf(redacted);
    }

    private static boolean isSensitive(String key) {
        String normalized = key.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        return SENSITIVE_FRAGMENTS.stream()
                .map(fragment -> fragment.replace("-", "").replace("_", ""))
                .anyMatch(normalized::contains);
    }

    private static Object safeValue(Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return String.valueOf(value);
    }
}

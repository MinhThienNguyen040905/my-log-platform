package com.mylog.insight.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Optional port that rewrites template insight sentences in a warmer tone. Implementations receive only
 * computed facts and the template draft — never journal text, IDs or user-written tag names — and must not
 * log them. Any failure returns an empty map so callers keep the template wording.
 */
public interface InsightNarrator {
    record Request(String key, String type, String direction, Map<String, Object> facts, String draft) {}

    record Result(Map<String, String> narratives, String provider, String model, String promptVersion,
                  int inputTokens, int outputTokens, BigDecimal estimatedCostUsd, long latencyMs, boolean succeeded) {
        public static Result failed(String provider, String model, String promptVersion, long latencyMs) {
            return new Result(Map.of(), provider, model, promptVersion, 0, 0, BigDecimal.ZERO, latencyMs, false);
        }
    }

    Result narrate(List<Request> requests);
}

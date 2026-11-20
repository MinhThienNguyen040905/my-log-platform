package com.mylog.analysis.application;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reads one journal entry and returns the sentences that match a {@link ThinkingPatternTaxonomy} code.
 * Implementations never throw for provider problems: they return {@link Result#failed} so the caller can
 * record usage and move on. Output is untrusted until {@link PatternSignalValidator} accepts it.
 */
public interface PatternExtractor {
    record Signal(String code, BigDecimal confidence, String quote) {}

    record Result(boolean succeeded, List<Signal> signals, String provider, String model, String version,
                  int inputTokens, int outputTokens, BigDecimal estimatedCostUsd) {
        public Result {
            signals = signals == null ? List.of() : List.copyOf(signals);
        }

        public static Result failed(String provider, String model, String version) {
            return new Result(false, List.of(), provider, model, version, 0, 0, BigDecimal.ZERO);
        }
    }

    Result extract(String title, String plainText);
}

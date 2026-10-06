package com.mylog.analysis.infrastructure;

import com.mylog.analysis.application.JournalAnalyzer;

import java.math.BigDecimal;
import java.util.List;

/** Synthetic local/test adapter. Enable explicitly; never sends user data to a network provider. */
class FakeJournalAnalyzer implements JournalAnalyzer {
    @Override public Result analyze(String title, String plainText) {
        return new Result("NEUTRAL", new BigDecimal("0.50000"),
                List.of(new ScoredCode("CALM", new BigDecimal("0.50000"))),
                List.of(new ScoredCode("OTHER", new BigDecimal("0.50000"))),
                "Bạn có thể dành một chút thời gian để suy ngẫm về trải nghiệm này.",
                "LOCAL_FAKE", "deterministic-v1", "1", "fake-v1", 0, 0, BigDecimal.ZERO);
    }
}

package com.mylog.analysis;

import com.mylog.analysis.application.AnalysisOutputValidator;
import com.mylog.analysis.application.JournalAnalyzer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisOutputValidatorTest {
    @Test void acceptsBoundedStructuredOutput() {
        assertDoesNotThrow(() -> AnalysisOutputValidator.validate(result("Một câu hỏi để suy ngẫm.")));
    }

    @Test void rejectsInventedHotlineAndDiagnosis() {
        assertThrows(AnalysisOutputValidator.InvalidAnalysisOutputException.class,
                () -> AnalysisOutputValidator.validate(result("Gọi hotline để được chẩn đoán.")));
    }

    @Test void rejectsOutOfRangeAndUnknownAggregateCode() {
        var bad = new JournalAnalyzer.Result("NEUTRAL", BigDecimal.ONE,
                List.of(new JournalAnalyzer.ScoredCode("UNAPPROVED", new BigDecimal("0.9"))),
                List.of(), "Suy ngẫm.", "FAKE", "fake", "1", "v1", 0, 0, BigDecimal.ZERO);
        assertThrows(AnalysisOutputValidator.InvalidAnalysisOutputException.class,
                () -> AnalysisOutputValidator.validate(bad));
    }

    private static JournalAnalyzer.Result result(String reflection) {
        return new JournalAnalyzer.Result("NEUTRAL", new BigDecimal("0.50000"),
                List.of(new JournalAnalyzer.ScoredCode("CALM", new BigDecimal("0.50000"))),
                List.of(new JournalAnalyzer.ScoredCode("OTHER", new BigDecimal("0.50000"))),
                reflection, "FAKE", "fake", "1", "v1", 0, 0, BigDecimal.ZERO);
    }
}

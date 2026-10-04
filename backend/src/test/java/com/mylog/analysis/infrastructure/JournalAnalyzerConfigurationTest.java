package com.mylog.analysis.infrastructure;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class JournalAnalyzerConfigurationTest {
    private final JournalAnalyzerConfiguration configuration = new JournalAnalyzerConfiguration();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void providerRequiresBothRecordedApprovals() {
        assertThrows(IllegalArgumentException.class, () -> analyzer(false, true));
        assertThrows(IllegalArgumentException.class, () -> analyzer(true, false));
        assertInstanceOf(OpenAiJournalAnalyzer.class, analyzer(true, true));
    }

    private com.mylog.analysis.application.JournalAnalyzer analyzer(boolean providerApproved,
                                                                     boolean outputApproved) {
        return configuration.journalAnalyzer(false, "openai",
                "https://api.openai.com/v1/chat/completions", "test-model", "test-key", 30000,
                BigDecimal.ONE, BigDecimal.ONE,
                providerApproved, outputApproved, mapper);
    }
}

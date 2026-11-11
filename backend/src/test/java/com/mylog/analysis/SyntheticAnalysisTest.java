package com.mylog.analysis;

import com.mylog.analysis.application.AnalysisOutputValidator;
import com.mylog.analysis.application.SyntheticAnalysis;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SyntheticAnalysisTest {
    @Test void followsTheUsersMoodIconAndPassesTheValidator() {
        for (int i = 0; i < 200; i++) {
            UUID id = UUID.randomUUID();
            var happy = SyntheticAnalysis.generate(new SyntheticAnalysis.Input(id, "happy",
                    new BigDecimal("8.2"), new BigDecimal("3.0"), 450, Set.of("RELATIONSHIPS")));
            AnalysisOutputValidator.validate(happy);
            assertTrue(Set.of("JOY", "HOPE").contains(happy.emotions().getFirst().code()));
            assertEquals("RELATIONSHIPS", happy.topics().getFirst().code());
            assertEquals("LOCAL_FAKE", happy.provider());

            var sad = SyntheticAnalysis.generate(new SyntheticAnalysis.Input(id, "sad",
                    new BigDecimal("3.5"), new BigDecimal("7.5"), 300, Set.of()));
            AnalysisOutputValidator.validate(sad);
            assertEquals("SADNESS", sad.emotions().getFirst().code());
            assertTrue(sad.emotions().stream().anyMatch(e -> e.code().equals("ANXIETY")));
            assertTrue(sad.topics().stream().anyMatch(t -> t.code().equals("SLEEP")));
        }
    }

    @Test void isDeterministicPerEntry() {
        UUID id = UUID.randomUUID();
        var input = new SyntheticAnalysis.Input(id, "neutral", new BigDecimal("6.0"), new BigDecimal("5.0"), null, Set.of());
        assertEquals(SyntheticAnalysis.generate(input), SyntheticAnalysis.generate(input));
        assertEquals("OTHER", SyntheticAnalysis.generate(input).topics().getFirst().code());
    }
}

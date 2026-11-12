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

    @Test void secondaryTonesNeverContradictTheIcon() {
        int neutralWithHope = 0;
        for (int i = 0; i < 600; i++) {
            UUID id = UUID.randomUUID();
            for (String icon : new String[] {"sad", "very-bad", "angry"}) {
                var negative = SyntheticAnalysis.generate(new SyntheticAnalysis.Input(id, icon,
                        new BigDecimal("3.5"), new BigDecimal("7.5"), 420, Set.of()));
                assertTrue(negative.emotions().stream().noneMatch(e -> Set.of("HOPE", "JOY", "CALM").contains(e.code())),
                        icon + " -> " + negative.emotions());
                assertNotEquals("MIXED", negative.sentiment());
            }
            var happy = SyntheticAnalysis.generate(new SyntheticAnalysis.Input(id, "happy",
                    new BigDecimal("8"), new BigDecimal("3"), 420, Set.of()));
            assertTrue(happy.emotions().stream().noneMatch(e -> Set.of("SADNESS", "FEAR", "ANGER").contains(e.code())));
            var neutral = SyntheticAnalysis.generate(new SyntheticAnalysis.Input(id, "neutral",
                    new BigDecimal("6"), new BigDecimal("4"), 420, Set.of()));
            assertEquals("CALM", neutral.emotions().getFirst().code());
            if (neutral.emotions().stream().anyMatch(e -> e.code().equals("HOPE"))) neutralWithHope++;
        }
        assertTrue(neutralWithHope < 600 * 0.25, "hope is only an occasional secondary tone on neutral days: " + neutralWithHope);
    }

    @Test void isDeterministicPerEntry() {
        UUID id = UUID.randomUUID();
        var input = new SyntheticAnalysis.Input(id, "neutral", new BigDecimal("6.0"), new BigDecimal("5.0"), null, Set.of());
        assertEquals(SyntheticAnalysis.generate(input), SyntheticAnalysis.generate(input));
        assertEquals("OTHER", SyntheticAnalysis.generate(input).topics().getFirst().code());
    }
}

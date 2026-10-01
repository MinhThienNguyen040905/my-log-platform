package com.mylog.safety;

import com.mylog.safety.application.RiskLevel;
import com.mylog.safety.application.RiskClassifier;
import com.mylog.safety.application.SafetyScreeningService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SafetyScreeningCorpusTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-30T00:00:00Z"), ZoneOffset.UTC);
    private final SafetyScreeningService screening = new SafetyScreeningService(text -> Optional.empty(),
            (rule, provider, version, confidence, at) -> Optional.empty(), CLOCK);

    @Test void syntheticEnglishAndVietnameseCorpus() {
        record Case(String text, RiskLevel expected, String decision) {}
        var cases = List.of(
                new Case("I want to die", RiskLevel.HIGH, "SAFETY_FLOW"),
                new Case("I will kill myself now", RiskLevel.CRITICAL, "SAFETY_FLOW"),
                new Case("Tôi muốn chết", RiskLevel.HIGH, "SAFETY_FLOW"),
                new Case("Tôi định tự tử tối nay", RiskLevel.CRITICAL, "SAFETY_FLOW"),
                new Case("Today was calm and quiet", RiskLevel.UNKNOWN, "FAIL_SAFE"),
                new Case("Tôi buồn và cần nghỉ ngơi", RiskLevel.UNKNOWN, "FAIL_SAFE"),
                // Contextual cases remain conservative until a reviewed classifier is available.
                new Case("I don't want to die", RiskLevel.HIGH, "SAFETY_FLOW"),
                new Case("The novel says 'I want to die'", RiskLevel.HIGH, "SAFETY_FLOW"),
                new Case("I said I was fine, but sure, everything is just great", RiskLevel.UNKNOWN, "FAIL_SAFE"),
                new Case("I'm done with all this", RiskLevel.UNKNOWN, "FAIL_SAFE")
        );
        for (var item : cases) {
            var result = screening.screen(item.text());
            assertEquals(item.expected(), result.riskLevel(), item.text());
            assertEquals(item.decision(), result.decision(), item.text());
        }
    }

    @Test void classifierFailureCannotEnableOrdinaryAnalysis() {
        var unavailable = new SafetyScreeningService(text -> { throw new IllegalStateException("unavailable"); },
                (rule, provider, version, confidence, at) -> Optional.empty(), CLOCK);
        assertEquals("FAIL_SAFE", unavailable.screen("A calm day").decision());
    }

    @Test void normalClassifierNeedsMatchingApprovedPolicy() {
        RiskClassifier classifier = text -> Optional.of(new RiskClassifier.Classification(
                "example", "v1", RiskLevel.NORMAL, new BigDecimal("0.98")));
        var withoutApproval = new SafetyScreeningService(classifier,
                (rule, provider, version, confidence, at) -> Optional.empty(), CLOCK);
        assertEquals("FAIL_SAFE", withoutApproval.screen("A calm day").decision());
        var withApproval = new SafetyScreeningService(classifier,
                (rule, provider, version, confidence, at) -> Optional.of("approved-v1"), CLOCK);
        var result = withApproval.screen("A calm day");
        assertEquals("ALLOW", result.decision());
        assertEquals("approved-v1", result.policyVersion());
    }
}

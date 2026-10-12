package com.mylog.safety.infrastructure;

import com.mylog.safety.application.SafetyScreeningService;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaselineSafetyConfigurationTest {
    private final BaselineSafetyConfiguration config = new BaselineSafetyConfiguration();
    private final SafetyScreeningService screening = new SafetyScreeningService(
            config.mvpRiskClassifier(), config.mvpSafetyPolicyGate("rules", ""), Clock.systemUTC());

    @Test void ordinarySyntheticTextIsAllowed() {
        var decision = screening.screen("Hôm nay mình đi học và thấy vui.");
        assertEquals("ALLOW", decision.decision());
        assertEquals("mvp-rules-v1", decision.policyVersion());
    }

    @Test void cautionAndHighRiskTextNeverGetOrdinaryAi() {
        assertEquals("CONSTRAIN", screening.screen("I am done with all this").decision());
        assertEquals("SAFETY_FLOW", screening.screen("Tôi muốn chết").decision());
    }
}

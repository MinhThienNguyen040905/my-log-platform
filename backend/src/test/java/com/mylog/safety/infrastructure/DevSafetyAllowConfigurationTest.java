package com.mylog.safety.infrastructure;

import com.mylog.safety.application.RiskLevel;
import com.mylog.safety.application.SafetyPolicyGate;
import com.mylog.safety.application.SafetyScreeningService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DevSafetyAllowConfigurationTest {
    private static MockEnvironment profile(String profile) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profile);
        return env;
    }

    @Test void refusesToStartOutsideLocalProfile() {
        assertThrows(IllegalStateException.class, () -> new DevSafetyAllowConfiguration(profile("prod"), false));
        assertThrows(IllegalStateException.class, () -> new DevSafetyAllowConfiguration(profile("staging"), false));
    }

    @Test void refusesToStartTogetherWithJobs() {
        assertThrows(IllegalStateException.class, () -> new DevSafetyAllowConfiguration(profile("local"), true));
    }

    @Test void onlyApprovesItsOwnProviderAndDelegatesEverythingElse() {
        var config = new DevSafetyAllowConfiguration(profile("local"), false);
        SafetyPolicyGate realGate = (rule, provider, version, confidence, at) -> Optional.empty();
        SafetyPolicyGate gate = config.devAllowSafetyPolicyGate(realGate);
        Instant now = Instant.now();
        assertTrue(gate.approvedPolicy("r", "dev-allow", "dev", BigDecimal.ONE, now).isPresent());
        assertTrue(gate.approvedPolicy("r", "real-classifier", "v1", BigDecimal.ONE, now).isEmpty());
    }

    @Test void keywordRulesStillRouteCrisisTextToSafetyFlow() {
        var config = new DevSafetyAllowConfiguration(profile("local"), false);
        var service = new SafetyScreeningService(config.devAllowRiskClassifier(),
                config.devAllowSafetyPolicyGate((r, p, v, c, a) -> Optional.empty()), Clock.systemUTC());
        assertEquals("ALLOW", service.screen("Hôm nay buổi họp kéo dài, mình hơi mệt.").decision());
        var crisis = service.screen("Nhiều lúc mình không muốn sống nữa.");
        assertEquals("SAFETY_FLOW", crisis.decision());
        assertNotEquals(RiskLevel.NORMAL, crisis.riskLevel());
    }
}

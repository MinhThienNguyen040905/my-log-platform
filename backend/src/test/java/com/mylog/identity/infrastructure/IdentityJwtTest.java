package com.mylog.identity.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IdentityJwtTest {
    @Test void previousPublicKeyVerifiesTokensDuringRotation() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair old = generator.generateKeyPair();
        KeyPair current = generator.generateKeyPair();
        var oldEnv = environment(old, "old-key");
        var newEnv = environment(current, "new-key").withProperty("MYLOG_JWT_PREVIOUS_PUBLIC_KEYS",
                "old-key:" + Base64.getEncoder().encodeToString(old.getPublic().getEncoded()));
        Clock clock = Clock.fixed(Instant.parse("2026-09-30T00:00:00Z"), ZoneOffset.UTC);
        var oldTokens = new IdentityJwt(oldEnv, clock, UUID::randomUUID);
        var currentTokens = new IdentityJwt(newEnv, clock, UUID::randomUUID);
        UUID user = UUID.randomUUID();
        String token = oldTokens.issue(user, UUID.randomUUID());
        assertEquals(user.toString(), currentTokens.decode(token).getSubject());
        assertThrows(org.springframework.security.oauth2.jwt.JwtException.class,
                () -> new IdentityJwt(environment(current, "new-key"), clock, UUID::randomUUID).decode(token));
    }

    private static MockEnvironment environment(KeyPair pair, String keyId) {
        var env = new MockEnvironment()
                .withProperty("MYLOG_JWT_PRIVATE_KEY", Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()))
                .withProperty("MYLOG_JWT_PUBLIC_KEY", Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()))
                .withProperty("MYLOG_JWT_KEY_ID", keyId);
        env.setActiveProfiles("test");
        return env;
    }
}

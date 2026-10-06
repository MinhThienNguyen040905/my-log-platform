package com.mylog.identity.infrastructure;

import com.mylog.identity.application.AccessTokens;
import com.mylog.platform.id.IdGenerator;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public final class IdentityJwt implements AccessTokens, JwtDecoder {
    private final JwtEncoder encoder;
    private final List<JwtDecoder> decoders;
    private final Clock clock;
    private final IdGenerator ids;
    private final String issuer;
    private final String audience;

    IdentityJwt(Environment environment, Clock clock, IdGenerator ids) {
        this.clock = clock;
        this.ids = ids;
        issuer = environment.getProperty("MYLOG_JWT_ISSUER", "mylog-local");
        audience = environment.getProperty("MYLOG_JWT_AUDIENCE", "mylog-api");
        try {
            KeyPair pair = keyPair(environment);
            String keyId = environment.getProperty("MYLOG_JWT_KEY_ID", "identity-v1");
            var jwk = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate()).keyID(keyId).build();
            encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
            var configured = new ArrayList<JwtDecoder>();
            configured.add(decoderFor((RSAPublicKey) pair.getPublic()));
            String previous = environment.getProperty("MYLOG_JWT_PREVIOUS_PUBLIC_KEYS", "");
            if (!previous.isBlank()) {
                for (String entry : previous.split(",")) {
                    String[] parts = entry.split(":", 2);
                    if (parts.length != 2 || parts[0].isBlank() || keyId.equals(parts[0]))
                        throw new IllegalStateException("Invalid previous JWT public key configuration");
                    var spec = new X509EncodedKeySpec(Base64.getDecoder().decode(parts[1]));
                    configured.add(decoderFor((RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec)));
                }
            }
            decoders = List.copyOf(configured);
        } catch (Exception e) { throw new IllegalStateException("Cannot initialize identity signing keys", e); }
    }

    @Override
    public String issue(UUID userId, UUID sessionId) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).audience(java.util.List.of(audience))
                .subject(userId.toString()).id(ids.next().toString())
                .claim("sid", sessionId.toString()).issuedAt(now).notBefore(now)
                .expiresAt(now.plus(Duration.ofMinutes(10))).build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @Override public Jwt decode(String token) throws JwtException {
        JwtException failure = null;
        for (JwtDecoder candidate : decoders) {
            try { return candidate.decode(token); }
            catch (JwtException e) { failure = e; }
        }
        throw failure == null ? new BadJwtException("No JWT verification key") : failure;
    }

    private JwtDecoder decoderFor(RSAPublicKey key) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(key).build();
        decoder.setJwtValidator(jwt -> {
            Instant now = clock.instant();
            if (!issuer.equals(jwt.getClaimAsString("iss"))
                    || !jwt.getAudience().contains(audience)
                    || jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(now)
                    || jwt.getNotBefore() != null && jwt.getNotBefore().isAfter(now.plusSeconds(30))) {
                return OAuth2TokenValidatorResult.failure(new org.springframework.security.oauth2.core.OAuth2Error("invalid_token"));
            }
            return OAuth2TokenValidatorResult.success();
        });
        return decoder;
    }

    private static KeyPair keyPair(Environment environment) throws Exception {
        String privatePem = environment.getProperty("MYLOG_JWT_PRIVATE_KEY");
        String publicPem = environment.getProperty("MYLOG_JWT_PUBLIC_KEY");
        if (privatePem == null || publicPem == null) {
            if (!environment.matchesProfiles("local", "test"))
                throw new IllegalStateException("JWT signing keys required outside local/test");
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        }
        KeyFactory factory = KeyFactory.getInstance("RSA");
        byte[] privateBytes = pem(privatePem);
        byte[] publicBytes = pem(publicPem);
        KeyPair pair = new KeyPair(factory.generatePublic(new X509EncodedKeySpec(publicBytes)),
                factory.generatePrivate(new PKCS8EncodedKeySpec(privateBytes)));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(pair.getPrivate());
        signature.update("mylog-key-pair-check".getBytes(StandardCharsets.UTF_8));
        byte[] signed = signature.sign();
        signature.initVerify(pair.getPublic());
        signature.update("mylog-key-pair-check".getBytes(StandardCharsets.UTF_8));
        if (!signature.verify(signed)) throw new IllegalStateException("JWT key pair does not match");
        return pair;
    }

    private static byte[] pem(String value) {
        String normalized = value.replace("\\n", "\n").replaceAll("-----[^-]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized.getBytes(StandardCharsets.US_ASCII));
    }
}

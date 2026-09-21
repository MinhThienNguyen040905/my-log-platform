package com.mylog.identity.application;

import com.mylog.identity.domain.RefreshToken;
import com.mylog.identity.domain.User;
import com.mylog.identity.domain.UserStatus;
import com.mylog.identity.infrastructure.persistence.RefreshTokenRepository;
import com.mylog.identity.infrastructure.persistence.UserRepository;
import com.mylog.shared.api.ApiErrorCodes;
import com.mylog.shared.exception.UnauthorizedException;
import com.mylog.shared.security.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final ClientFingerprintService fingerprintService;
    private final Clock clock;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            JwtTokenService jwtTokenService,
            JwtProperties jwtProperties,
            ClientFingerprintService fingerprintService,
            Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
        this.fingerprintService = fingerprintService;
        this.clock = clock;
    }

    @Transactional
    public IssuedTokens createSession(User user, ClientMetadata client) {
        return issue(user, UUID.randomUUID(), client);
    }

    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public IssuedTokens rotate(String rawToken, ClientMetadata client) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(this::invalidToken);

        if (current.isRevoked()) {
            refreshTokenRepository.revokeActiveFamily(current.getFamilyId(), now);
            throw new RefreshTokenReuseException();
        }
        if (current.isExpired(now)) {
            current.revoke(now, null);
            throw invalidToken();
        }

        User user = userRepository.findById(current.getUserId())
                .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(this::invalidToken);
        String nextRawToken = generateRawToken();
        UUID nextId = UUID.randomUUID();
        RefreshToken next = createToken(nextId, user.getId(), current.getFamilyId(), nextRawToken, client, now);
        current.revoke(now, nextId);
        refreshTokenRepository.save(next);

        JwtTokenService.AccessToken accessToken = jwtTokenService.issue(user);
        return new IssuedTokens(
                accessToken.value(), accessToken.expiresAt(), nextRawToken, user.getId());
    }

    @Transactional
    public void revokeSession(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token ->
                refreshTokenRepository.revokeActiveFamily(token.getFamilyId(), clock.instant()));
    }

    private IssuedTokens issue(User user, UUID familyId, ClientMetadata client) {
        Instant now = clock.instant();
        String rawToken = generateRawToken();
        refreshTokenRepository.save(createToken(
                UUID.randomUUID(), user.getId(), familyId, rawToken, client, now));
        JwtTokenService.AccessToken accessToken = jwtTokenService.issue(user);
        return new IssuedTokens(
                accessToken.value(), accessToken.expiresAt(), rawToken, user.getId());
    }

    private RefreshToken createToken(
            UUID id,
            UUID userId,
            UUID familyId,
            String rawToken,
            ClientMetadata client,
            Instant now) {
        return new RefreshToken(
                id,
                userId,
                familyId,
                hash(rawToken),
                now.plus(jwtProperties.refreshTokenTtl()),
                fingerprintService.hash(client.ipAddress()),
                fingerprintService.hash(client.userAgent()),
                now);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidToken();
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private UnauthorizedException invalidToken() {
        return new UnauthorizedException(
                ApiErrorCodes.INVALID_REFRESH_TOKEN, "Refresh token is invalid");
    }
}

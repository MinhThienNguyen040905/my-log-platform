package com.mylog.identity.application;

import com.mylog.identity.configuration.PolicyProperties;
import com.mylog.identity.domain.User;
import com.mylog.identity.domain.UserConsent;
import com.mylog.identity.domain.UserPreference;
import com.mylog.identity.domain.UserStatus;
import com.mylog.identity.infrastructure.persistence.UserConsentRepository;
import com.mylog.identity.infrastructure.persistence.UserPreferenceRepository;
import com.mylog.identity.infrastructure.persistence.UserRepository;
import com.mylog.shared.api.ApiErrorCodes;
import com.mylog.shared.exception.ConflictException;
import com.mylog.shared.exception.UnauthorizedException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final UserConsentRepository consentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNormalizer emailNormalizer;
    private final RefreshTokenService refreshTokenService;
    private final PolicyProperties policyProperties;
    private final Clock clock;
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository,
            UserPreferenceRepository preferenceRepository,
            UserConsentRepository consentRepository,
            PasswordEncoder passwordEncoder,
            EmailNormalizer emailNormalizer,
            RefreshTokenService refreshTokenService,
            PolicyProperties policyProperties,
            Clock clock) {
        this.userRepository = userRepository;
        this.preferenceRepository = preferenceRepository;
        this.consentRepository = consentRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailNormalizer = emailNormalizer;
        this.refreshTokenService = refreshTokenService;
        this.policyProperties = policyProperties;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode("MyLog-Dummy-Password-Only-For-Timing");
    }

    @Transactional
    public AuthenticationResult register(
            String email,
            String password,
            String displayName,
            ClientMetadata client) {
        String normalizedEmail = emailNormalizer.normalize(email);
        if (userRepository.existsByEmailNormalized(normalizedEmail)) {
            throw duplicateEmail();
        }

        Instant now = clock.instant();
        User user = new User(
                UUID.randomUUID(),
                email.trim(),
                normalizedEmail,
                passwordEncoder.encode(password),
                displayName.trim(),
                now);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateEmail();
        }

        UserPreference preference = new UserPreference(user.getId(), "UTC", "vi", now);
        preferenceRepository.save(preference);
        consentRepository.saveAll(List.of(
                new UserConsent(
                        UUID.randomUUID(), user.getId(), "TERMS", policyProperties.termsVersion(), now),
                new UserConsent(
                        UUID.randomUUID(), user.getId(), "PRIVACY", policyProperties.privacyVersion(), now)));

        IssuedTokens tokens = refreshTokenService.createSession(user, client);
        return new AuthenticationResult(tokens, toProfile(user, preference));
    }

    @Transactional
    public AuthenticationResult login(String email, String password, ClientMetadata client) {
        var candidate = userRepository.findByEmailNormalized(emailNormalizer.normalize(email));
        String passwordHash = candidate.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(password, passwordHash);
        if (candidate.isEmpty()
                || candidate.get().getStatus() != UserStatus.ACTIVE
                || !passwordMatches) {
            throw invalidCredentials();
        }
        User user = candidate.get();

        user.recordLogin(clock.instant());
        UserPreference preference = preferenceRepository.findById(user.getId())
                .orElseThrow(this::invalidCredentials);
        IssuedTokens tokens = refreshTokenService.createSession(user, client);
        return new AuthenticationResult(tokens, toProfile(user, preference));
    }

    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public AuthenticationResult refresh(String rawToken, ClientMetadata client) {
        IssuedTokens tokens = refreshTokenService.rotate(rawToken, client);
        UUID userId = tokens.userId();
        User user = userRepository.findById(userId).orElseThrow(this::invalidCredentials);
        UserPreference preference = preferenceRepository.findById(userId)
                .orElseThrow(this::invalidCredentials);
        return new AuthenticationResult(tokens, toProfile(user, preference));
    }

    public void logout(String rawToken) {
        refreshTokenService.revokeSession(rawToken);
    }

    private UserProfile toProfile(User user, UserPreference preference) {
        return new UserProfile(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getPlan(),
                preference.getTimezone(),
                preference.getLanguage(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    private ConflictException duplicateEmail() {
        return new ConflictException("EMAIL_ALREADY_EXISTS", "Email is already registered");
    }

    private UnauthorizedException invalidCredentials() {
        return new UnauthorizedException(
                ApiErrorCodes.INVALID_CREDENTIALS, "Email or password is invalid");
    }
}

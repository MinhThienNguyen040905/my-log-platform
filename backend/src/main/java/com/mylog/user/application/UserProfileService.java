package com.mylog.user.application;

import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.web.ConflictException;
import com.mylog.platform.web.ResourceNotFoundException;
import com.mylog.platform.web.InvalidRequestException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class UserProfileService implements UserProfileUseCase {
    private static final Set<String> CONSENT_TYPES = Set.of("TERMS", "PRIVACY", "AI_PROCESSING", "ANALYTICS", "MODEL_TRAINING");
    private static final Set<String> WELLNESS_GOALS = Set.of("REFLECTION", "SLEEP", "MINDFULNESS", "EXERCISE", "SOCIAL");
    private final UserStore store;
    private final SensitiveDataCipher cipher;
    private final IdGenerator ids;
    private final Clock clock;
    private final ObjectMapper mapper;

    public UserProfileService(UserStore store, SensitiveDataCipher cipher, IdGenerator ids, Clock clock, ObjectMapper mapper) {
        this.store = store; this.cipher = cipher; this.ids = ids; this.clock = clock; this.mapper = mapper;
    }

    private record ProfilePayload(String displayName, String penName, List<String> onboardingGoals) {}

    @Override @Transactional
    public void createDefault(UUID userId, String timezone, String locale, String termsVersion,
                              String privacyVersion, Instant now) {
        String zone = validTimezone(timezone);
        String language = validLocale(locale);
        store.create(new UserStore.Profile(userId, encrypted(userId, new ProfilePayload("", "", List.of())),
                zone, language, null, 0), now);
        store.addConsent(ids.next(), userId, "TERMS", termsVersion, true, "ONBOARDING", now);
        store.addConsent(ids.next(), userId, "PRIVACY", privacyVersion, true, "ONBOARDING", now);
        for (String optional : List.of("AI_PROCESSING", "ANALYTICS", "MODEL_TRAINING"))
            store.addConsent(ids.next(), userId, optional, "v1", false, "ONBOARDING", now);
    }

    @Override @Transactional(readOnly = true)
    public ProfileView get(UUID userId) { return view(require(userId)); }

    @Override @Transactional
    public ProfileView update(UUID userId, String displayName, String penName, List<String> onboardingGoals, String timezone,
                              String locale, long expectedVersion) {
        UserStore.Profile current = require(userId);
        if (current.version() != expectedVersion) throw new ConflictException("Hồ sơ đã được cập nhật ở nơi khác.");
        ProfilePayload old = decrypted(current);
        if (onboardingGoals != null && onboardingGoals.stream().anyMatch(java.util.Objects::isNull))
            throw new InvalidRequestException();
        List<String> goals = onboardingGoals == null ?
                (old.onboardingGoals() == null ? List.of() : old.onboardingGoals()) : List.copyOf(onboardingGoals);
        if (goals.size() > 5 || !WELLNESS_GOALS.containsAll(goals) || new java.util.HashSet<>(goals).size() != goals.size())
            throw new InvalidRequestException();
        var updated = new ProfilePayload(displayName == null ? old.displayName() : displayName.trim(),
                penName == null ? old.penName() : penName.trim(), goals);
        if (updated.displayName().length() > 120 || updated.penName().length() > 120)
            throw new InvalidRequestException();
        var next = new UserStore.Profile(userId, encrypted(userId, updated),
                timezone == null ? current.timezone() : validTimezone(timezone),
                locale == null ? current.locale() : validLocale(locale), current.onboardingCompletedAt(), expectedVersion + 1);
        if (!store.update(next, expectedVersion, clock.instant())) throw new ConflictException("Hồ sơ đã được cập nhật ở nơi khác.");
        return view(next);
    }

    @Override @Transactional
    public ProfileView completeOnboarding(UUID userId, long expectedVersion) {
        UserStore.Profile current = require(userId);
        Instant now = clock.instant();
        if (!store.completeOnboarding(userId, expectedVersion, now)) throw new ConflictException("Hồ sơ đã được cập nhật ở nơi khác.");
        return view(new UserStore.Profile(userId, current.payload(), current.timezone(), current.locale(), now, expectedVersion + 1));
    }

    @Override @Transactional
    public void decideConsent(UUID userId, String type, String version, boolean granted) {
        require(userId);
        if (!CONSENT_TYPES.contains(type) || version == null || version.isBlank() || version.length() > 40)
            throw new InvalidRequestException();
        store.addConsent(ids.next(), userId, type, version, granted, "SETTINGS", clock.instant());
    }

    @Override @Transactional(readOnly = true)
    public List<ConsentView> consents(UUID userId) {
        require(userId);
        return store.latestConsents(userId).stream().map(c -> new ConsentView(c.type(), c.documentVersion(), c.granted(), c.decidedAt())).toList();
    }

    @Override @Transactional(readOnly = true)
    public boolean isConsentGranted(UUID userId, String type) {
        return consents(userId).stream().anyMatch(c -> c.type().equals(type) && c.granted());
    }

    private UserStore.Profile require(UUID userId) {
        return store.find(userId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ."));
    }
    private ProfileView view(UserStore.Profile profile) {
        ProfilePayload data = decrypted(profile);
        return new ProfileView(profile.userId(), data.displayName(), data.penName(), data.onboardingGoals(), profile.timezone(),
                profile.locale(), profile.onboardingCompletedAt(), profile.version());
    }
    private SensitiveDataCipher.Encrypted encrypted(UUID userId, ProfilePayload payload) {
        return cipher.encrypt("user_profiles.profile", userId, userId, mapper.writeValueAsString(payload));
    }
    private ProfilePayload decrypted(UserStore.Profile profile) {
        return mapper.readValue(cipher.decrypt("user_profiles.profile", profile.userId(), profile.userId(), profile.payload()), ProfilePayload.class);
    }
    private static String validTimezone(String value) {
        try { return ZoneId.of(value).getId(); }
        catch (RuntimeException e) { throw new InvalidRequestException(); }
    }
    private static String validLocale(String value) {
        if (value == null || !value.matches("[a-z]{2}(-[A-Z]{2})?")) throw new InvalidRequestException();
        return Locale.forLanguageTag(value).toLanguageTag();
    }
}

package com.mylog.identity.service;

import com.mylog.identity.entity.User;
import com.mylog.identity.entity.UserPreference;
import com.mylog.identity.entity.UserStatus;
import com.mylog.identity.repository.UserPreferenceRepository;
import com.mylog.identity.repository.UserRepository;
import com.mylog.common.api.ApiErrorCodes;
import com.mylog.common.exception.BadRequestException;
import com.mylog.common.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final Clock clock;

    public UserProfileService(
            UserRepository userRepository,
            UserPreferenceRepository preferenceRepository,
            Clock clock) {
        this.userRepository = userRepository;
        this.preferenceRepository = preferenceRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserProfile get(UUID authenticatedUserId) {
        return load(authenticatedUserId);
    }

    @Transactional
    public UserProfile update(
            UUID authenticatedUserId,
            String displayName,
            String timezone,
            String language) {
        User user = activeUser(authenticatedUserId);
        UserPreference preference = preferenceRepository.findById(authenticatedUserId)
                .orElseThrow(this::profileNotFound);
        if (timezone != null) {
            validateTimezone(timezone);
        }
        if (displayName != null) {
            user.updateDisplayName(displayName.trim(), clock.instant());
        }
        preference.update(timezone, language, clock.instant());
        return toProfile(user, preference);
    }

    private UserProfile load(UUID userId) {
        User user = activeUser(userId);
        UserPreference preference = preferenceRepository.findById(userId)
                .orElseThrow(this::profileNotFound);
        return toProfile(user, preference);
    }

    private User activeUser(UUID id) {
        return userRepository.findById(id)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(this::profileNotFound);
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (ZoneRulesException exception) {
            throw new BadRequestException(ApiErrorCodes.INVALID_REQUEST, "Timezone is invalid");
        }
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

    private ResourceNotFoundException profileNotFound() {
        return new ResourceNotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "User profile not found");
    }
}

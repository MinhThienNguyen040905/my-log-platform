package com.mylog.identity.infrastructure.security;

import com.mylog.identity.domain.UserStatus;
import com.mylog.identity.infrastructure.persistence.UserRepository;
import com.mylog.shared.security.AccountStatusVerifier;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaAccountStatusVerifier implements AccountStatusVerifier {

    private final UserRepository userRepository;

    public JpaAccountStatusVerifier(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isActive(UUID userId) {
        return userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE);
    }
}

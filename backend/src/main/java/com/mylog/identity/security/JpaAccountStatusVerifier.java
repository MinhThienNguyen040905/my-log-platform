package com.mylog.identity.security;

import com.mylog.identity.entity.UserStatus;
import com.mylog.identity.repository.UserRepository;
import com.mylog.common.security.AccountStatusVerifier;
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

package com.mylog.identity.infrastructure.persistence;

import com.mylog.identity.domain.User;
import com.mylog.identity.domain.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailNormalized(String emailNormalized);

    boolean existsByEmailNormalized(String emailNormalized);

    boolean existsByIdAndStatus(UUID id, UserStatus status);
}

package com.mylog.identity.repository;

import com.mylog.identity.entity.User;
import com.mylog.identity.entity.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailNormalized(String emailNormalized);

    boolean existsByEmailNormalized(String emailNormalized);

    boolean existsByIdAndStatus(UUID id, UserStatus status);
}

package com.mylog.identity.infrastructure.persistence;

import com.mylog.identity.domain.UserPreference;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {}

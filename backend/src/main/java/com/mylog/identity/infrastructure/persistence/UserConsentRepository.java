package com.mylog.identity.infrastructure.persistence;

import com.mylog.identity.domain.UserConsent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {}

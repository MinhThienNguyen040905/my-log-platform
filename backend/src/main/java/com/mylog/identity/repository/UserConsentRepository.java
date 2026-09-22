package com.mylog.identity.repository;

import com.mylog.identity.entity.UserConsent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {}

package com.mylog.selfcare.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "selfcare_goals")
class SelfCareGoal {
    @Id UUID id;
    @Column(name = "user_id", nullable = false) UUID userId;
    @Column(nullable = false) String category;
    @Column(name = "encrypted_title", nullable = false) byte[] encryptedTitle;
    @Column(name = "title_iv", nullable = false) byte[] titleIv;
    @Column(name = "title_wrapped_key", nullable = false) byte[] titleWrappedKey;
    @Column(name = "title_key_version", nullable = false) String titleKeyVersion;
    @Column(name = "encrypted_description") byte[] encryptedDescription;
    @Column(name = "description_iv") byte[] descriptionIv;
    @Column(name = "description_wrapped_key") byte[] descriptionWrappedKey;
    @Column(name = "description_key_version") String descriptionKeyVersion;
    @Column(nullable = false) String status;
    @Column(name = "start_date") LocalDate startDate;
    @Column(name = "target_date") LocalDate targetDate;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Column(name = "completed_at") Instant completedAt;
    @Column(name = "row_version", nullable = false) long rowVersion;
}

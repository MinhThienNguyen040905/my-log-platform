package com.mylog.selfcare.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "selfcare_goals")
public class SelfCareGoal {
    @Id public UUID id;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(nullable = false) public String category;
    @Column(name = "encrypted_title", nullable = false) public byte[] encryptedTitle;
    @Column(name = "title_iv", nullable = false) public byte[] titleIv;
    @Column(name = "title_wrapped_key", nullable = false) public byte[] titleWrappedKey;
    @Column(name = "title_key_version", nullable = false) public String titleKeyVersion;
    @Column(name = "encrypted_description") public byte[] encryptedDescription;
    @Column(name = "description_iv") public byte[] descriptionIv;
    @Column(name = "description_wrapped_key") public byte[] descriptionWrappedKey;
    @Column(name = "description_key_version") public String descriptionKeyVersion;
    @Column(nullable = false) public String status;
    @Column(name = "start_date") public LocalDate startDate;
    @Column(name = "target_date") public LocalDate targetDate;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt;
    @Column(name = "completed_at") public Instant completedAt;
    @Column(name = "row_version", nullable = false) public long rowVersion;
}
